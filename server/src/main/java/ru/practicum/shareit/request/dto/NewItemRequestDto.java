package ru.practicum.shareit.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Новый запрос вещи: только описание, его уже проверил gateway. Автора и время создания добавит сервер.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewItemRequestDto {

    private String description;
}
