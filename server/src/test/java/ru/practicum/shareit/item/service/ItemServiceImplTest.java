package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ItemRequestRepository requestRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private final User owner = User.builder().id(1L).name("Owner").email("owner@mail.ru").build();
    private final User booker = User.builder().id(2L).name("Booker").email("booker@mail.ru").build();
    private final Item item = Item.builder()
            .id(10L).name("Дрель").description("Ударная").available(true).owner(owner).build();

    @Test
    void shouldCreateItemWithoutRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        ItemDto result = itemService.create(1L, ItemDto.builder()
                .name("Дрель").description("Ударная").available(true).build());

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getRequestId()).isNull();
        verifyNoInteractions(requestRepository);
    }

    @Test
    void shouldLinkNewItemToRequest() {
        ItemRequest request = ItemRequest.builder().id(3L).description("Нужна дрель").requestor(booker).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(requestRepository.findById(3L)).thenReturn(Optional.of(request));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        ItemDto result = itemService.create(1L, ItemDto.builder()
                .name("Дрель").description("Ударная").available(true).requestId(3L).build());

        assertThat(result.getRequestId()).isEqualTo(3L);
        ArgumentCaptor<Item> saved = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(saved.capture());
        assertThat(saved.getValue().getRequest()).isEqualTo(request);
        assertThat(saved.getValue().getOwner()).isEqualTo(owner);
    }

    @Test
    void shouldNotCreateItemForUnknownRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.create(1L, ItemDto.builder()
                .name("Дрель").description("Ударная").available(true).requestId(99L).build()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Item request with id=99 not found");
        verify(itemRepository, never()).save(any());
    }

    @Test
    void shouldNotCreateItemForUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.create(99L, ItemDto.builder().name("Дрель").build()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldUpdateOnlySentFieldsOfOwnItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);

        ItemDto result = itemService.update(1L, 10L, ItemDto.builder().available(false).build());

        assertThat(result.getAvailable()).isFalse();
        assertThat(result.getName()).isEqualTo("Дрель");
        assertThat(result.getDescription()).isEqualTo("Ударная");
    }

    @Test
    void shouldUpdateNameAndDescription() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);

        ItemDto result = itemService.update(1L, 10L, ItemDto.builder().name("Шуруповёрт").description("Новый").build());

        assertThat(result.getName()).isEqualTo("Шуруповёрт");
        assertThat(result.getDescription()).isEqualTo("Новый");
        assertThat(result.getAvailable()).isTrue();
    }

    @Test
    void shouldNotLetStrangerUpdateItem() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.update(2L, 10L, ItemDto.builder().name("Чужая").build()))
                .isInstanceOf(ForbiddenException.class);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenItemNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getById(1L, 99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Item with id=99 not found");
    }

    @Test
    void shouldShowLastAndNextBookingsToOwner() {
        LocalDateTime now = LocalDateTime.now();
        Booking past = booking(100L, now.minusDays(3), now.minusDays(2));
        Booking current = booking(101L, now.minusHours(1), now.plusHours(1));
        Booking nearFuture = booking(102L, now.plusDays(1), now.plusDays(2));
        Booking farFuture = booking(103L, now.plusDays(5), now.plusDays(6));
        Comment comment = Comment.builder()
                .id(7L).text("Отличная дрель").item(item).author(booker).created(now.minusDays(1)).build();
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.findByItem_IdInAndStatus(List.of(10L), BookingStatus.APPROVED))
                .thenReturn(List.of(farFuture, past, current, nearFuture));
        when(commentRepository.findByItem_IdInOrderByCreated(List.of(10L))).thenReturn(List.of(comment));

        ItemWithBookingsDto result = itemService.getById(1L, 10L);

        // последняя — та, что началась позже всех из начавшихся; следующая — ближайшая из будущих
        assertThat(result.getLastBooking().getId()).isEqualTo(101L);
        assertThat(result.getNextBooking().getId()).isEqualTo(102L);
        assertThat(result.getComments()).extracting(CommentDto::getAuthorName).containsExactly("Booker");
    }

    @Test
    void shouldHideBookingsFromOtherUsers() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentRepository.findByItem_IdInOrderByCreated(List.of(10L))).thenReturn(List.of());

        ItemWithBookingsDto result = itemService.getById(null, 10L);

        assertThat(result.getLastBooking()).isNull();
        assertThat(result.getNextBooking()).isNull();
        assertThat(result.getComments()).isEmpty();
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void shouldReturnOwnerItemsWithBookingsAndComments() {
        Item secondItem = Item.builder().id(11L).name("Пила").description("Ручная").available(true).owner(owner).build();
        LocalDateTime now = LocalDateTime.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findByOwner_IdOrderById(1L)).thenReturn(List.of(item, secondItem));
        when(bookingRepository.findByItem_IdInAndStatus(List.of(10L, 11L), BookingStatus.APPROVED))
                .thenReturn(List.of(booking(100L, now.plusDays(1), now.plusDays(2))));
        when(commentRepository.findByItem_IdInOrderByCreated(List.of(10L, 11L))).thenReturn(List.of());

        List<ItemWithBookingsDto> result = itemService.getAllByOwner(1L);

        assertThat(result).extracting(ItemWithBookingsDto::getId).containsExactly(10L, 11L);
        assertThat(result.get(0).getNextBooking().getBookerId()).isEqualTo(2L);
        assertThat(result.get(1).getNextBooking()).isNull();
        assertThat(result.get(1).getComments()).isEmpty();
    }

    @Test
    void shouldNotQueryBookingsForOwnerWithoutItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findByOwner_IdOrderById(1L)).thenReturn(List.of());

        assertThat(itemService.getAllByOwner(1L)).isEmpty();
        verifyNoInteractions(bookingRepository, commentRepository);
    }

    @Test
    void shouldReturnEmptyResultForBlankSearch() {
        assertThat(itemService.search("   ")).isEmpty();
        assertThat(itemService.search(null)).isEmpty();
        verify(itemRepository, never()).searchAvailable(anyString());
    }

    @Test
    void shouldSearchAvailableItems() {
        when(itemRepository.searchAvailable("дрель")).thenReturn(List.of(item));

        assertThat(itemService.search("дрель")).extracting(ItemDto::getName).containsExactly("Дрель");
    }

    @Test
    void shouldAddCommentAfterFinishedBooking() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                eq(10L), eq(2L), eq(BookingStatus.APPROVED), any(LocalDateTime.class))).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(7L);
            return comment;
        });

        CommentDto result = itemService.addComment(2L, 10L, CommentDto.builder().text("Отличная дрель").build());

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getText()).isEqualTo("Отличная дрель");
        assertThat(result.getAuthorName()).isEqualTo("Booker");
        assertThat(result.getCreated()).isNotNull();
    }

    @Test
    void shouldRejectCommentWithoutFinishedBooking() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                eq(10L), eq(2L), eq(BookingStatus.APPROVED), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> itemService.addComment(2L, 10L, CommentDto.builder().text("Хочу").build()))
                .isInstanceOf(ValidationException.class);
        verify(commentRepository, never()).save(any());
    }

    private Item withId(Item newItem) {
        newItem.setId(10L);
        return newItem;
    }

    private Booking booking(Long id, LocalDateTime start, LocalDateTime end) {
        return Booking.builder()
                .id(id).start(start).end(end).item(item).booker(booker).status(BookingStatus.APPROVED).build();
    }
}
