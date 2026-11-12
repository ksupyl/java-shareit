package ru.practicum.shareit.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Вещь в списке ответов на запрос: id, название и владелец. Подробности — через GET /items/{id}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemShortDto {

    private Long id;
    private String name;
    private Long ownerId;
}
