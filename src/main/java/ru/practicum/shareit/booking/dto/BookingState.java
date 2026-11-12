package ru.practicum.shareit.booking.dto;

/**
 * Какие брони показать в списке. Это фильтр поиска, а не статус брони:
 * CURRENT, PAST и FUTURE считаются по датам, WAITING и REJECTED — по статусу.
 */
public enum BookingState {
    ALL,      // все
    CURRENT,  // идут сейчас
    PAST,     // уже закончились
    FUTURE,   // ещё не начались
    WAITING,  // ждут решения владельца
    REJECTED  // отклонены владельцем
}
