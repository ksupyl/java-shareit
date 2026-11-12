package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "start");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private final User owner = User.builder().id(1L).name("Owner").email("owner@mail.ru").build();
    private final User booker = User.builder().id(2L).name("Booker").email("booker@mail.ru").build();
    private final Item item = Item.builder()
            .id(10L).name("Дрель").description("Ударная").available(true).owner(owner).build();
    private final LocalDateTime start = LocalDateTime.of(2030, 1, 10, 10, 0);
    private final LocalDateTime end = LocalDateTime.of(2030, 1, 11, 10, 0);

    @Test
    void shouldCreateWaitingBooking() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(100L);
            return booking;
        });

        BookingDto result = bookingService.create(2L, new NewBookingDto(10L, start, end));

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(result.getStart()).isEqualTo(start);
        assertThat(result.getEnd()).isEqualTo(end);
        assertThat(result.getBooker().getId()).isEqualTo(2L);
        assertThat(result.getItem().getName()).isEqualTo("Дрель");
    }

    @Test
    void shouldNotBookForUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(99L, new NewBookingDto(10L, start, end)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(itemRepository);
    }

    @Test
    void shouldNotBookUnknownItem() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(2L, new NewBookingDto(99L, start, end)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Item with id=99 not found");
    }

    @Test
    void shouldNotBookUnavailableItem() {
        item.setAvailable(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(2L, new NewBookingDto(10L, start, end)))
                .isInstanceOf(ValidationException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldNotLetOwnerBookOwnItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(1L, new NewBookingDto(10L, start, end)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Owner cannot book their own item");
    }

    @Test
    void shouldApproveWaitingBooking() {
        Booking booking = booking(BookingStatus.WAITING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        assertThat(bookingService.approve(1L, 100L, true).getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void shouldRejectWaitingBooking() {
        Booking booking = booking(BookingStatus.WAITING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        assertThat(bookingService.approve(1L, 100L, false).getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void shouldNotLetStrangerDecide() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking(BookingStatus.WAITING)));

        assertThatThrownBy(() -> bookingService.approve(2L, 100L, true)).isInstanceOf(ForbiddenException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldNotDecideTwice() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking(BookingStatus.APPROVED)));

        assertThatThrownBy(() -> bookingService.approve(1L, 100L, false))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Booking id=100 is already APPROVED");
    }

    @Test
    void shouldShowBookingToBookerAndOwnerOnly() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking(BookingStatus.WAITING)));

        assertThat(bookingService.getById(2L, 100L).getId()).isEqualTo(100L);
        assertThat(bookingService.getById(1L, 100L).getId()).isEqualTo(100L);
        assertThatThrownBy(() -> bookingService.getById(3L, 100L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldThrowWhenBookingNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getById(2L, 99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Booking with id=99 not found");
    }

    @Test
    void shouldReturnBookerBookingsAsDtos() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(bookingRepository.findByBooker_Id(2L, NEWEST_FIRST)).thenReturn(List.of(booking(BookingStatus.WAITING)));

        List<BookingDto> result = bookingService.getAllByBooker(2L, BookingState.ALL);

        assertThat(result).extracting(BookingDto::getId).containsExactly(100L);
    }

    // Сами выборки проверяет интеграционный тест на базе. Здесь — что сервис зовёт нужную выборку для каждого state
    @ParameterizedTest
    @EnumSource(BookingState.class)
    void shouldChooseQueryForBookerByState(BookingState state) {
        when(userRepository.existsById(2L)).thenReturn(true);

        bookingService.getAllByBooker(2L, state);

        switch (state) {
            case ALL -> verify(bookingRepository).findByBooker_Id(2L, NEWEST_FIRST);
            case CURRENT -> verify(bookingRepository)
                    .findByBooker_IdAndStartIsBeforeAndEndIsAfter(eq(2L), any(), any(), eq(NEWEST_FIRST));
            case PAST -> verify(bookingRepository).findByBooker_IdAndEndIsBefore(eq(2L), any(), eq(NEWEST_FIRST));
            case FUTURE -> verify(bookingRepository).findByBooker_IdAndStartIsAfter(eq(2L), any(), eq(NEWEST_FIRST));
            case WAITING -> verify(bookingRepository)
                    .findByBooker_IdAndStatus(2L, BookingStatus.WAITING, NEWEST_FIRST);
            case REJECTED -> verify(bookingRepository)
                    .findByBooker_IdAndStatus(2L, BookingStatus.REJECTED, NEWEST_FIRST);
        }
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void shouldChooseQueryForOwnerByState(BookingState state) {
        when(userRepository.existsById(1L)).thenReturn(true);

        bookingService.getAllByOwner(1L, state);

        switch (state) {
            case ALL -> verify(bookingRepository).findByItem_Owner_Id(1L, NEWEST_FIRST);
            case CURRENT -> verify(bookingRepository)
                    .findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(eq(1L), any(), any(), eq(NEWEST_FIRST));
            case PAST -> verify(bookingRepository).findByItem_Owner_IdAndEndIsBefore(eq(1L), any(), eq(NEWEST_FIRST));
            case FUTURE -> verify(bookingRepository)
                    .findByItem_Owner_IdAndStartIsAfter(eq(1L), any(), eq(NEWEST_FIRST));
            case WAITING -> verify(bookingRepository)
                    .findByItem_Owner_IdAndStatus(1L, BookingStatus.WAITING, NEWEST_FIRST);
            case REJECTED -> verify(bookingRepository)
                    .findByItem_Owner_IdAndStatus(1L, BookingStatus.REJECTED, NEWEST_FIRST);
        }
    }

    @Test
    void shouldUseSameMomentForBothEdgesOfCurrentBookings() {
        when(userRepository.existsById(2L)).thenReturn(true);

        bookingService.getAllByBooker(2L, BookingState.CURRENT);

        // "сейчас" для начала и конца брони — одно и то же мгновение, иначе граница могла бы разъехаться
        ArgumentCaptor<LocalDateTime> startBefore = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endAfter = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(bookingRepository).findByBooker_IdAndStartIsBeforeAndEndIsAfter(
                eq(2L), startBefore.capture(), endAfter.capture(), eq(NEWEST_FIRST));
        assertThat(startBefore.getValue()).isEqualTo(endAfter.getValue());
    }

    @Test
    void shouldNotListBookingsOfUnknownUser() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.getAllByBooker(99L, BookingState.ALL))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> bookingService.getAllByOwner(99L, BookingState.ALL))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(bookingRepository);
    }

    private Booking booking(BookingStatus status) {
        return Booking.builder().id(100L).start(start).end(end).item(item).booker(booker).status(status).build();
    }
}
