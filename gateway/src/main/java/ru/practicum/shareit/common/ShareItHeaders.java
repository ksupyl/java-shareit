package ru.practicum.shareit.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Свои HTTP-заголовки API.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShareItHeaders {

    // id пользователя, от имени которого пришёл запрос
    public static final String USER_ID = "X-Sharer-User-Id";
}
