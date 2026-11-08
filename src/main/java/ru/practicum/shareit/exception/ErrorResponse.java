package ru.practicum.shareit.exception;

/**
 * Тело любого ответа с ошибкой: {"error": "текст ошибки"}.
 */
public record ErrorResponse(String error) {
}