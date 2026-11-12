package ru.practicum.shareit.request.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemRequestMapper {

    /**
     * Запрос вместе с вещами, которые добавили в ответ на него. Вещи ищет сервис: у маппера нет базы.
     */
    public static ItemRequestDto toItemRequestDto(ItemRequest request, List<ItemShortDto> items) {
        return ItemRequestDto.builder()
                .id(request.getId())
                .description(request.getDescription())
                .created(request.getCreated())
                .items(items)
                .build();
    }

    /**
     * Новый запрос от пользователя. id выдаст база, время создания передаёт сервис.
     */
    public static ItemRequest toItemRequest(NewItemRequestDto requestDto, User requestor, LocalDateTime created) {
        return ItemRequest.builder()
                .description(requestDto.getDescription())
                .requestor(requestor)
                .created(created)
                .build();
    }
}
