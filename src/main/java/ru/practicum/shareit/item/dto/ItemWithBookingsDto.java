package ru.practicum.shareit.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.booking.dto.BookingShortDto;

import java.util.List;

/**
 * Вещь при просмотре: к её полям добавлены последняя и ближайшая брони и отзывы.
 * Брони видит только владелец, у остальных эти поля null.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemWithBookingsDto {

    private Long id;
    private String name;
    private String description;
    private Boolean available;
    private Long requestId;
    private BookingShortDto lastBooking;
    private BookingShortDto nextBooking;
    private List<CommentDto> comments;
}
