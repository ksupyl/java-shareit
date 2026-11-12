package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    // списки броней по ТЗ — от новых к старым
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "start");

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingDto create(Long bookerId, NewBookingDto bookingDto) {
        // даты уже проверил gateway, здесь — только то, для чего нужна база
        User booker = getUserOrThrow(bookerId);
        Item item = itemRepository.findById(bookingDto.getItemId())
                .orElseThrow(() -> new NotFoundException("Item with id=" + bookingDto.getItemId() + " not found"));
        if (!item.isAvailable()) {
            throw new ValidationException("Item with id=" + item.getId() + " is not available for booking");
        }
        if (item.getOwner().getId().equals(bookerId)) {
            throw new ValidationException("Owner cannot book their own item");
        }
        Booking booking = BookingMapper.toBooking(bookingDto, item, booker);
        // новая бронь всегда ждёт решения владельца
        booking.setStatus(BookingStatus.WAITING);
        Booking saved = bookingRepository.save(booking);
        log.info("Created booking id={} for item id={} by user id={}", saved.getId(), item.getId(), bookerId);
        return BookingMapper.toBookingDto(saved);
    }

    @Override
    @Transactional
    public BookingDto approve(Long ownerId, Long bookingId, boolean approved) {
        Booking booking = getBookingOrThrow(bookingId);
        // пользователя в базе не ищем: решает только владелец вещи, и сравнения id достаточно
        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Only the item owner can approve booking id=" + bookingId);
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Booking id=" + bookingId + " is already " + booking.getStatus());
        }
        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking id={} is {}", bookingId, saved.getStatus());
        return BookingMapper.toBookingDto(saved);
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new ForbiddenException("Only the booker or the item owner can view booking id=" + bookingId);
        }
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getAllByBooker(Long bookerId, BookingState state) {
        checkUserExists(bookerId);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByBooker_Id(bookerId, NEWEST_FIRST);
            case CURRENT -> bookingRepository
                    .findByBooker_IdAndStartIsBeforeAndEndIsAfter(bookerId, now, now, NEWEST_FIRST);
            case PAST -> bookingRepository.findByBooker_IdAndEndIsBefore(bookerId, now, NEWEST_FIRST);
            case FUTURE -> bookingRepository.findByBooker_IdAndStartIsAfter(bookerId, now, NEWEST_FIRST);
            case WAITING -> bookingRepository
                    .findByBooker_IdAndStatus(bookerId, BookingStatus.WAITING, NEWEST_FIRST);
            case REJECTED -> bookingRepository
                    .findByBooker_IdAndStatus(bookerId, BookingStatus.REJECTED, NEWEST_FIRST);
        };
        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    @Override
    public List<BookingDto> getAllByOwner(Long ownerId, BookingState state) {
        checkUserExists(ownerId);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByItem_Owner_Id(ownerId, NEWEST_FIRST);
            case CURRENT -> bookingRepository
                    .findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(ownerId, now, now, NEWEST_FIRST);
            case PAST -> bookingRepository.findByItem_Owner_IdAndEndIsBefore(ownerId, now, NEWEST_FIRST);
            case FUTURE -> bookingRepository.findByItem_Owner_IdAndStartIsAfter(ownerId, now, NEWEST_FIRST);
            case WAITING -> bookingRepository
                    .findByItem_Owner_IdAndStatus(ownerId, BookingStatus.WAITING, NEWEST_FIRST);
            case REJECTED -> bookingRepository
                    .findByItem_Owner_IdAndStatus(ownerId, BookingStatus.REJECTED, NEWEST_FIRST);
        };
        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
    }

    private Booking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking with id=" + bookingId + " not found"));
    }
}
