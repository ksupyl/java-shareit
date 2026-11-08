package ru.practicum.shareit.booking.model;

public enum BookingStatus {
    WAITING, // ждёт решения владельца
    APPROVED, // владелец подтвердил
    REJECTED, // владелец отказал
    CANCELED // арендатор отменил сам
}