package ru.practicum.shareit.item.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.common.ShareItFormats;

import java.time.LocalDateTime;

/**
 * Отзыв в запросе и в ответе. Клиент присылает только text, остальные поля заполняет сервер.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;
    private String text;
    private String authorName;

    @JsonFormat(pattern = ShareItFormats.DATE_TIME)
    private LocalDateTime created;
}
