package ru.practicum.shareit.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Превращает исключения из контроллеров и сервисов в HTTP-статус и JSON с текстом ошибки.
 */
@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("400 Bad Request: {}", message);
        return new ErrorResponse(message);
    }

    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            ValidationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(Exception e) {
        log.warn("400 Bad Request: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    // Параметр не того типа: /items/abc или ?state=SOON. Текст Spring тут с именами Java-классов,
    // поэтому отвечаем своим.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'";
        log.warn("400 Bad Request: {}", message);
        return new ErrorResponse(message);
    }

    // Тело запроса не прочитать: его нет, JSON битый или в поле не тот тип, например дата "tomorrow".
    // Текст Spring и Jackson тут с именами Java-классов и методов, поэтому он только в лог.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("400 Bad Request: {}", e.getMessage());
        return new ErrorResponse("Request body is missing or malformed");
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
