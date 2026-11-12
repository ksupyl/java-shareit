package ru.practicum.shareit.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Превращает исключения сервисов в HTTP-статус и JSON с текстом ошибки.
 * Форму запроса уже проверил gateway, поэтому здесь только ошибки, для которых нужна база:
 * нарушенные правила сервиса, чужие данные, ненайденные записи и конфликты.
 */
@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    // Запрос верный по форме, но нарушает правила сервиса: бронь недоступной вещи, отзыв без аренды
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(ValidationException e) {
        log.warn("400 Bad Request: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(ForbiddenException e) {
        log.warn("403 Forbidden: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException e) {
        log.warn("404 Not Found: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(ConflictException e) {
        log.warn("409 Conflict: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    // Запись нарушила ограничение базы. Например, два запроса с одним email пришли одновременно:
    // проверку в сервисе прошли оба, а вторую вставку отклонил UNIQUE. Текст ошибки базы — только в лог.
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("409 Conflict: {}", e.getMostSpecificCause().getMessage());
        return new ErrorResponse("Data conflicts with existing records");
    }

    // Сюда долетают только баги. Ловим RuntimeException, а не Exception: свои ошибки
    // Spring (неизвестный адрес, неверный HTTP-метод) — не RuntimeException, и они
    // сохранят обычные 404 и 405, а не превратятся в 500.
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(RuntimeException e) {
        log.error("500 Internal Server Error", e);
        return new ErrorResponse("Internal server error");
    }
}
