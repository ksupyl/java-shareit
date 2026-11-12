package ru.practicum.shareit.booking.dto;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON брони: даты до секунды, статус строкой, вещь и арендатор вложенными объектами.
 */
@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class BookingDtoJsonTest {

    private final JacksonTester<BookingDto> json;
    private final JacksonTester<NewBookingDto> newBookingJson;

    @Test
    void shouldWriteBooking() throws Exception {
        BookingDto booking = BookingDto.builder()
                .id(100L)
                .start(LocalDateTime.of(2030, 1, 10, 10, 0, 0, 300_000_000))
                .end(LocalDateTime.of(2030, 1, 11, 10, 0))
                .status(BookingStatus.WAITING)
                .booker(new UserDto(2L, "Booker", "booker@mail.ru"))
                .item(new ItemDto(10L, "Дрель", "Ударная", true, null))
                .build();

        JsonContent<BookingDto> result = json.write(booking);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(100);
        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-10T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-11T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("WAITING");
        assertThat(result).extractingJsonPathNumberValue("$.booker.id").isEqualTo(2);
        assertThat(result).extractingJsonPathStringValue("$.item.name").isEqualTo("Дрель");
    }

    @Test
    void shouldReadBookingFromGateway() throws Exception {
        NewBookingDto result = newBookingJson.parseObject(
                "{\"itemId\":10,\"start\":\"2030-01-10T10:00:00\",\"end\":\"2030-01-11T10:00:00\"}");

        assertThat(result.getItemId()).isEqualTo(10L);
        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 10, 10, 0));
        assertThat(result.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 11, 10, 0));
    }
}
