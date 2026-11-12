package ru.practicum.shareit.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.common.ShareItFormats;

import java.time.LocalDateTime;

/**
 * Бронь внутри карточки вещи: кто и на какие даты. Саму вещь не повторяем.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingShortDto {

    private Long id;
    private Long bookerId;

    @JsonFormat(pattern = ShareItFormats.DATE_TIME)
    private LocalDateTime start;

    @JsonFormat(pattern = ShareItFormats.DATE_TIME)
    private LocalDateTime end;
}
