package ru.practicum.shareit.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // все даты в ответах API — до секунды, в одном формате: 2026-11-12T10:00:00
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime start;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime end;
}
