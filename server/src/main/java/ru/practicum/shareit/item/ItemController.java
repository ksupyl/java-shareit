package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@RequestHeader(ShareItHeaders.USER_ID) Long userId, @RequestBody ItemDto itemDto) {
        log.info("POST /items, user id={}", userId);
        return itemService.create(userId, itemDto);
    }

    @PatchMapping("/{itemId}")
    public ItemDto update(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                          @PathVariable Long itemId,
                          @RequestBody ItemDto itemDto) {
        log.info("PATCH /items/{}, user id={}", itemId, userId);
        return itemService.update(userId, itemId, itemDto);
    }

    // Заголовок необязательный: вещь может посмотреть кто угодно, а брони в ней увидит только владелец
    @GetMapping("/{itemId}")
    public ItemWithBookingsDto getById(@RequestHeader(value = ShareItHeaders.USER_ID, required = false) Long userId,
                                       @PathVariable Long itemId) {
        log.info("GET /items/{}, user id={}", itemId, userId);
        return itemService.getById(userId, itemId);
    }

    @GetMapping
    public List<ItemWithBookingsDto> getAllByOwner(@RequestHeader(ShareItHeaders.USER_ID) Long userId) {
        log.info("GET /items, user id={}", userId);
        return itemService.getAllByOwner(userId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestParam String text) {
        log.info("GET /items/search, text={}", text);
        return itemService.search(text);
    }

    @PostMapping("/{itemId}/comment")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                 @PathVariable Long itemId,
                                 @RequestBody CommentDto commentDto) {
        log.info("POST /items/{}/comment, user id={}", itemId, userId);
        return itemService.addComment(userId, itemId, commentDto);
    }
}
