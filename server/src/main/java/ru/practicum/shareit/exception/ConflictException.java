package ru.practicum.shareit.exception;

/**
 * Запрос спорит с тем, что уже есть в системе. Например, такой email уже занят.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
