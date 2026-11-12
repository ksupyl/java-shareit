package ru.practicum.shareit.exception;

/**
 * Запрос нарушает правила сервиса, хотя по форме он верный. Например, бронь недоступной вещи.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
