package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.validation.NullOrNotBlank;
import ru.practicum.shareit.validation.OnCreate;
import ru.practicum.shareit.validation.OnUpdate;

/**
 * Вещь в запросе на создание и изменение. id сюда не входит: его выдаёт сервер.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {

    @NotBlank(groups = OnCreate.class)
    @NullOrNotBlank(groups = OnUpdate.class)
    @Size(max = 255, groups = {OnCreate.class, OnUpdate.class})
    private String name;

    @NotBlank(groups = OnCreate.class)
    @NullOrNotBlank(groups = OnUpdate.class)
    @Size(max = 1000, groups = {OnCreate.class, OnUpdate.class})
    private String description;

    // Boolean, а не boolean: "не прислали" станет null, и его поймает @NotNull
    @NotNull(groups = OnCreate.class)
    private Boolean available;

    // id запроса, в ответ на который добавляют вещь; можно не присылать
    private Long requestId;
}
