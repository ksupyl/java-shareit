package ru.practicum.shareit.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.common.ShareItFormats;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

/**
 * Бронь в ответах API: вещь и арендатор приходят целиком, а не одними id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {

    private Long id;

    @JsonFormat(pattern = ShareItFormats.DATE_TIME)
    private LocalDateTime start;

    @JsonFormat(pattern = ShareItFormats.DATE_TIME)
    private LocalDateTime end;

    private BookingStatus status;
    private UserDto booker;
    private ItemDto item;
}
