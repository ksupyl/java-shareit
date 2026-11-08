package ru.practicum.shareit.exception;

/**
 * Пользователь существует, но это действие ему недоступно. Например, правка чужой вещи.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
