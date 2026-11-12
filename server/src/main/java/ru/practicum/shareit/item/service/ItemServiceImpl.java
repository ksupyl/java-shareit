package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository requestRepository;

    @Override
    @Transactional
    public ItemDto create(Long ownerId, ItemDto itemDto) {
        User owner = getUserOrThrow(ownerId);
        // вещь могут добавить в ответ на чей-то запрос — тогда этот запрос должен существовать
        ItemRequest request = itemDto.getRequestId() != null ? getRequestOrThrow(itemDto.getRequestId()) : null;
        Item item = itemRepository.save(ItemMapper.toItem(itemDto, owner, request));
        log.info("Created item id={} for owner id={}", item.getId(), ownerId);
        return ItemMapper.toItemDto(item);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        User user = getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);
        if (!item.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the owner can edit item id=" + itemId);
        }
        ItemMapper.updateItemFields(item, itemDto);
        Item updated = itemRepository.save(item);
        log.info("Updated item id={}", itemId);
        return ItemMapper.toItemDto(updated);
    }

    @Override
    public ItemWithBookingsDto getById(Long userId, Long itemId) {
        Item item = getItemOrThrow(itemId);
        // userId == null — запрос без заголовка, такой пользователь точно не владелец
        boolean isOwner = item.getOwner().getId().equals(userId);
        return toItemsWithBookings(List.of(item), isOwner).get(0);
    }

    @Override
    public List<ItemWithBookingsDto> getAllByOwner(Long ownerId) {
        User owner = getUserOrThrow(ownerId);
        List<Item> items = itemRepository.findByOwner_IdOrderById(owner.getId());
        return toItemsWithBookings(items, true);
    }

    @Override
    public List<ItemDto> search(String text) {
        // пустой запрос совпал бы с каждой вещью, поэтому сразу отдаём пустой список
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.searchAvailable(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        User author = getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);
        LocalDateTime now = LocalDateTime.now();
        // отзыв принимаем только от того, кто брал вещь, и только когда аренда уже закончилась
        boolean hasFinishedBooking = bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                itemId, userId, BookingStatus.APPROVED, now);
        if (!hasFinishedBooking) {
            throw new ValidationException("User id=" + userId + " has no finished booking of item id=" + itemId);
        }
        Comment comment = commentRepository.save(CommentMapper.toComment(commentDto, item, author, now));
        log.info("Added comment id={} to item id={} by user id={}", comment.getId(), itemId, userId);
        return CommentMapper.toCommentDto(comment);
    }

    /**
     * Данные для карточек берём сразу для всех вещей, одним запросом на таблицу, а не запросом на каждую вещь:
     * иначе на список из N вещей ушёл бы N + 1 запрос.
     */
    private List<ItemWithBookingsDto> toItemsWithBookings(List<Item> items, boolean withBookings) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();
        Map<Long, List<Booking>> bookingsByItemId = withBookings ? findApprovedBookings(itemIds) : Map.of();
        Map<Long, List<CommentDto>> commentsByItemId = findComments(itemIds);
        LocalDateTime now = LocalDateTime.now();
        return items.stream()
                .map(item -> {
                    List<Booking> bookings = bookingsByItemId.getOrDefault(item.getId(), List.of());
                    return ItemMapper.toItemWithBookingsDto(item,
                            findLastBooking(bookings, now), findNextBooking(bookings, now),
                            commentsByItemId.getOrDefault(item.getId(), List.of()));
                })
                .toList();
    }

    // в карточке показываем только подтверждённые брони: ожидающие и отклонённые ещё ничего не значат
    private Map<Long, List<Booking>> findApprovedBookings(List<Long> itemIds) {
        return bookingRepository.findByItem_IdInAndStatus(itemIds, BookingStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));
    }

    private Map<Long, List<CommentDto>> findComments(List<Long> itemIds) {
        return commentRepository.findByItem_IdInOrderByCreated(itemIds).stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentDto, Collectors.toList())));
    }

    // последняя — та, что началась позже всех среди уже начавшихся
    private static BookingShortDto findLastBooking(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> !booking.getStart().isAfter(now))
                .max(Comparator.comparing(Booking::getStart))
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);
    }

    // следующая — та, что начнётся раньше всех среди ещё не начавшихся
    private static BookingShortDto findNextBooking(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> booking.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item with id=" + itemId + " not found"));
    }

    private ItemRequest getRequestOrThrow(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Item request with id=" + requestId + " not found"));
    }
}
