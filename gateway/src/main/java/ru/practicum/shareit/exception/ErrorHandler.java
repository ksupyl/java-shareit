package ru.practicum.shareit.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Ошибки, которые gateway находит сам, не спрашивая сервер: неверные данные в запросе
 * и недоступный сервер. Ответы сервера с ошибкой сюда не попадают — BaseClient отдаёт их клиенту как есть.
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
            MissingServletRequestParameterException.class
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

    // Сервер не ответил: не запущен или недоступен по сети. Клиент тут ни при чём, и это не баг
    // gateway, поэтому не 400 и не 500, а 503 — сервис временно недоступен.
    @ExceptionHandler(ResourceAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleServerUnavailable(ResourceAccessException e) {
        log.error("503 Service Unavailable: {}", e.getMessage());
        return new ErrorResponse("ShareIt server is unavailable");
    }

    // Сюда долетают только баги gateway. Как и на сервере, ловим RuntimeException, а не Exception:
    // свои ошибки Spring (неизвестный адрес, неверный HTTP-метод) сохранят обычные 404 и 405.
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(RuntimeException e) {
        log.error("500 Internal Server Error", e);
        return new ErrorResponse("Internal server error");
    }
}
