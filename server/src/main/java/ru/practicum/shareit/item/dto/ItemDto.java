package ru.practicum.shareit.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Вещь в ответах сервера и в запросах от gateway. Поля уже проверил gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {

    private Long id;
    private String name;
    private String description;
    // Boolean, а не boolean: в PATCH null значит "не менять"
    private Boolean available;
    private Long requestId;
}
