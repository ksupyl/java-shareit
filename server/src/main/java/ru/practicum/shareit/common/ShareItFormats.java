package ru.practicum.shareit.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Форматы данных в ответах API.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShareItFormats {

    // Дата и время до секунды: 2026-11-12T10:00:00. Без общего формата ответ на POST показал бы
    // наносекунды из Java, а ответ на GET — микросекунды из базы, и одно и то же время выглядело бы по-разному.
    public static final String DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss";
}
