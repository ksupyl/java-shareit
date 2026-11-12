package ru.practicum.shareit.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Новый запрос вещи: пользователь описывает, что ему нужно. Автора и время создания добавит сервер.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewItemRequestDto {

    // 1000 — длина колонки description в таблице requests
    @NotBlank
    @Size(max = 1000)
    private String description;
}
