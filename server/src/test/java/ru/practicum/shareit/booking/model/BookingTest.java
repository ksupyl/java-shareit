package ru.practicum.shareit.booking.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingTest {

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        Booking booking = Booking.builder().id(1L).status(BookingStatus.WAITING).build();
        Booking sameBooking = Booking.builder().id(1L).status(BookingStatus.APPROVED).build();

        assertThat(booking).isEqualTo(sameBooking);
        assertThat(booking).hasSameHashCodeAs(sameBooking);
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
        assertThat(Booking.builder().id(1L).build()).isNotEqualTo(Booking.builder().id(2L).build());
    }

    @Test
    void shouldEqualOnlyItselfWithoutId() {
        Booking newBooking = Booking.builder().status(BookingStatus.WAITING).build();

        assertThat(newBooking).isEqualTo(newBooking);
        assertThat(newBooking).isNotEqualTo(Booking.builder().status(BookingStatus.WAITING).build());
        assertThat(newBooking).isNotEqualTo(null);
        assertThat(newBooking).isNotEqualTo(BookingStatus.WAITING);
    }
}
