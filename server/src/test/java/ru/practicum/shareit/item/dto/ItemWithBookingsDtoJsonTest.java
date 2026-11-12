package ru.practicum.shareit.item.dto;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookingShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON карточки вещи: даты брони и отзыва до секунды, а пустая бронь — явный null, а не пропавшее поле.
 */
@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ItemWithBookingsDtoJsonTest {

    private final JacksonTester<ItemWithBookingsDto> json;
    private final JacksonTester<CommentDto> commentJson;

    @Test
    void shouldWriteItemCard() throws Exception {
        ItemWithBookingsDto card = ItemWithBookingsDto.builder()
                .id(10L)
                .name("Дрель")
                .description("Ударная")
                .available(true)
                .lastBooking(new BookingShortDto(100L, 2L,
                        LocalDateTime.of(2026, 11, 1, 10, 0, 0, 500_000_000), LocalDateTime.of(2026, 11, 2, 10, 0)))
                .comments(List.of(new CommentDto(7L, "Отличная дрель", "Booker",
                        LocalDateTime.of(2026, 11, 3, 12, 0, 15, 123_456_789))))
                .build();

        JsonContent<ItemWithBookingsDto> result = json.write(card);

        assertThat(result).extractingJsonPathNumberValue("$.lastBooking.id").isEqualTo(100);
        assertThat(result).extractingJsonPathNumberValue("$.lastBooking.bookerId").isEqualTo(2);
        assertThat(result).extractingJsonPathStringValue("$.lastBooking.start").isEqualTo("2026-11-01T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.lastBooking.end").isEqualTo("2026-11-02T10:00:00");
        // Postman проверяет, что поле nextBooking есть и равно null
        assertThat(result).hasJsonPath("$.nextBooking");
        assertThat(result).extractingJsonPathValue("$.nextBooking").isNull();
        assertThat(result).hasJsonPath("$.requestId");
        assertThat(result).extractingJsonPathStringValue("$.comments[0].created").isEqualTo("2026-11-03T12:00:15");
    }

    @Test
    void shouldReadCommentTextFromGateway() throws Exception {
        CommentDto result = commentJson.parseObject("{\"text\":\"Отличная дрель\"}");

        assertThat(result.getText()).isEqualTo("Отличная дрель");
        assertThat(result.getId()).isNull();
        assertThat(result.getCreated()).isNull();
    }
}
