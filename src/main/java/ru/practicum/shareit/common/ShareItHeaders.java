package ru.practicum.shareit.common;

/**
 * Свои HTTP-заголовки API.
 */
public final class ShareItHeaders {

    // id пользователя, от имени которого пришёл запрос
    public static final String USER_ID = "X-Sharer-User-Id";

    private ShareItHeaders() {
    }
}
