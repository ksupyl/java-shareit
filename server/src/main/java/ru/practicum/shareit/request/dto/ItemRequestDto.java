package ru.practicum.shareit.request.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.common.ShareItFormats;
import ru.practicum.shareit.item.dto.ItemShortDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Запрос вещи в ответах сервера: что ищут, когда попросили и какие вещи уже предложили в ответ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequestDto {

    private Long id;
    private String description;

    @JsonFormat(pattern = ShareItFormats.DATE_TIME)
    private LocalDateTime created;

    private List<ItemShortDto> items;
}
