package ru.practicum.shareit.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Запрос на бронирование: какую вещь и на какие даты. Даты уже проверил gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewBookingDto {

    private Long itemId;
    private LocalDateTime start;
    private LocalDateTime end;
}
