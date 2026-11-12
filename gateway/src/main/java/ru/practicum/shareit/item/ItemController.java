package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.validation.OnCreate;
import ru.practicum.shareit.validation.OnUpdate;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemClient itemClient;

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                         @RequestBody @Validated(OnCreate.class) ItemDto itemDto) {
        log.info("POST /items, user id={}", userId);
        return itemClient.create(userId, itemDto);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<Object> update(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                         @PathVariable Long itemId,
                                         @RequestBody @Validated(OnUpdate.class) ItemDto itemDto) {
        log.info("PATCH /items/{}, user id={}", itemId, userId);
        return itemClient.update(userId, itemId, itemDto);
    }

    // Заголовок необязательный: вещь может посмотреть кто угодно, а брони в ней увидит только владелец
    @GetMapping("/{itemId}")
    public ResponseEntity<Object> getById(@RequestHeader(value = ShareItHeaders.USER_ID, required = false) Long userId,
                                          @PathVariable Long itemId) {
        log.info("GET /items/{}, user id={}", itemId, userId);
        return itemClient.getById(userId, itemId);
    }

    @GetMapping
    public ResponseEntity<Object> getAllByOwner(@RequestHeader(ShareItHeaders.USER_ID) Long userId) {
        log.info("GET /items, user id={}", userId);
        return itemClient.getAllByOwner(userId);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam String text) {
        log.info("GET /items/search, text={}", text);
        // пустой поиск совпал бы с каждой вещью: отвечаем пустым списком сразу и сервер не беспокоим
        if (text.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        return itemClient.search(text);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> addComment(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                             @PathVariable Long itemId,
                                             @RequestBody @Valid CommentDto commentDto) {
        log.info("POST /items/{}/comment, user id={}", itemId, userId);
        return itemClient.addComment(userId, itemId, commentDto);
    }
}
