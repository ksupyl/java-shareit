package ru.practicum.shareit.booking.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class BookingServiceImplIntegrationTest {

    private final EntityManager em;
    private final BookingService bookingService;

    @Test
    void shouldCreateBookingAndThenApproveIt() {
        User owner = persistUser("Owner", "owner@mail.ru");
        User booker = persistUser("Booker", "booker@mail.ru");
        Item drill = persistItem(owner);
        LocalDateTime start = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS);

        BookingDto created = bookingService.create(booker.getId(), new NewBookingDto(drill.getId(), start,
                start.plusDays(1)));
        bookingService.approve(owner.getId(), created.getId(), true);

        Booking saved = em.find(Booking.class, created.getId());
        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(saved.getStart()).isEqualTo(start);
        assertThat(saved.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void shouldShowBookingOnlyToBookerAndItemOwner() {
        User owner = persistUser("Owner", "owner@mail.ru");
        User booker = persistUser("Booker", "booker@mail.ru");
        User stranger = persistUser("Stranger", "stranger@mail.ru");
        LocalDateTime now = LocalDateTime.now();
        Booking booking = persistBooking(persistItem(owner), booker, now.plusDays(1), now.plusDays(2),
                BookingStatus.WAITING);

        BookingDto result = bookingService.getById(booker.getId(), booking.getId());

        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(result.getBooker().getName()).isEqualTo("Booker");
        assertThat(result.getItem().getName()).isEqualTo("Дрель");
        // владелец вещи видит ту же бронь, а посторонний — нет
        assertThat(bookingService.getById(owner.getId(), booking.getId())).isEqualTo(result);
        assertThatThrownBy(() -> bookingService.getById(stranger.getId(), booking.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldFilterBookerBookingsByState() {
        User owner = persistUser("Owner", "owner@mail.ru");
        User booker = persistUser("Booker", "booker@mail.ru");
        Item drill = persistItem(owner);
        LocalDateTime now = LocalDateTime.now();
        Booking past = persistBooking(drill, booker, now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED);
        Booking current = persistBooking(drill, booker, now.minusHours(1), now.plusHours(1), BookingStatus.APPROVED);
        Booking waiting = persistBooking(drill, booker, now.plusDays(1), now.plusDays(2), BookingStatus.WAITING);
        Booking rejected = persistBooking(drill, booker, now.plusDays(3), now.plusDays(4), BookingStatus.REJECTED);

        // списки — от новых к старым по дате начала
        assertThat(bookingService.getAllByBooker(booker.getId(), BookingState.ALL)).extracting(BookingDto::getId)
                .containsExactly(rejected.getId(), waiting.getId(), current.getId(), past.getId());
        assertThat(bookingService.getAllByBooker(booker.getId(), BookingState.CURRENT)).extracting(BookingDto::getId)
                .containsExactly(current.getId());
        assertThat(bookingService.getAllByBooker(booker.getId(), BookingState.PAST)).extracting(BookingDto::getId)
                .containsExactly(past.getId());
        assertThat(bookingService.getAllByBooker(booker.getId(), BookingState.FUTURE)).extracting(BookingDto::getId)
                .containsExactly(rejected.getId(), waiting.getId());
        assertThat(bookingService.getAllByBooker(booker.getId(), BookingState.WAITING)).extracting(BookingDto::getId)
                .containsExactly(waiting.getId());
        assertThat(bookingService.getAllByBooker(booker.getId(), BookingState.REJECTED)).extracting(BookingDto::getId)
                .containsExactly(rejected.getId());
    }

    @Test
    void shouldShowOwnerOnlyBookingsOfOwnItems() {
        User owner = persistUser("Owner", "owner@mail.ru");
        User booker = persistUser("Booker", "booker@mail.ru");
        Item ownItem = persistItem(owner);
        Item foreignItem = persistItem(booker);
        LocalDateTime now = LocalDateTime.now();
        Booking first = persistBooking(ownItem, booker, now.plusDays(1), now.plusDays(2), BookingStatus.WAITING);
        Booking second = persistBooking(ownItem, booker, now.plusDays(5), now.plusDays(6), BookingStatus.APPROVED);
        persistBooking(foreignItem, owner, now.plusDays(1), now.plusDays(2), BookingStatus.WAITING);

        assertThat(bookingService.getAllByOwner(owner.getId(), BookingState.ALL)).extracting(BookingDto::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(bookingService.getAllByOwner(owner.getId(), BookingState.WAITING)).extracting(BookingDto::getId)
                .containsExactly(first.getId());
    }

    private User persistUser(String name, String email) {
        User user = User.builder().name(name).email(email).build();
        em.persist(user);
        return user;
    }

    private Item persistItem(User owner) {
        Item item = Item.builder().name("Дрель").description("Ударная").available(true).owner(owner).build();
        em.persist(item);
        return item;
    }

    private Booking persistBooking(Item item, User booker, LocalDateTime start, LocalDateTime end,
                                   BookingStatus status) {
        Booking booking = Booking.builder().item(item).booker(booker).start(start).end(end).status(status).build();
        em.persist(booking);
        return booking;
    }
}
