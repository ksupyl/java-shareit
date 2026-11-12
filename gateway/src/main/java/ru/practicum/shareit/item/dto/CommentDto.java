package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Отзыв в запросе: клиент присылает только текст, автора и дату добавит сервер.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    @NotBlank
    @Size(max = 1000)
    private String text;
}
