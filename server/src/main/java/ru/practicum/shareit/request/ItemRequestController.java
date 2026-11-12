package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {

    private final ItemRequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemRequestDto create(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                 @RequestBody NewItemRequestDto requestDto) {
        log.info("POST /requests, user id={}", userId);
        return requestService.create(userId, requestDto);
    }

    @GetMapping
    public List<ItemRequestDto> getOwn(@RequestHeader(ShareItHeaders.USER_ID) Long userId) {
        log.info("GET /requests, user id={}", userId);
        return requestService.getOwn(userId);
    }

    // "/all" — отдельный адрес, а не id: Spring выбирает его раньше, чем шаблон "/{requestId}"
    @GetMapping("/all")
    public List<ItemRequestDto> getAllOthers(@RequestHeader(ShareItHeaders.USER_ID) Long userId) {
        log.info("GET /requests/all, user id={}", userId);
        return requestService.getAllOthers(userId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getById(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                  @PathVariable Long requestId) {
        log.info("GET /requests/{}, user id={}", requestId, userId);
        return requestService.getById(userId, requestId);
    }
}
