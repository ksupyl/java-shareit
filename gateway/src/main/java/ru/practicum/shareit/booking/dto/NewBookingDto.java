package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.validation.StartBeforeEnd;

import java.time.LocalDateTime;

/**
 * Запрос на бронирование: какую вещь и на какие даты. Конец брони должен быть позже начала.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@StartBeforeEnd
public class NewBookingDto {

    // id в базе начинаются с единицы, так что 0 и минус отсекаем здесь, не спрашивая сервер
    @NotNull
    @Positive
    private Long itemId;

    @NotNull
    @FutureOrPresent
    private LocalDateTime start;

    @NotNull
    @Future
    private LocalDateTime end;
}
