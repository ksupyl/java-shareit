package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.request.dto.NewItemRequestDto;

@Slf4j
@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {

    private final ItemRequestClient requestClient;

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                         @RequestBody @Valid NewItemRequestDto requestDto) {
        log.info("POST /requests, user id={}", userId);
        return requestClient.create(userId, requestDto);
    }

    @GetMapping
    public ResponseEntity<Object> getOwn(@RequestHeader(ShareItHeaders.USER_ID) Long userId) {
        log.info("GET /requests, user id={}", userId);
        return requestClient.getOwn(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<Object> getAllOthers(@RequestHeader(ShareItHeaders.USER_ID) Long userId) {
        log.info("GET /requests/all, user id={}", userId);
        return requestClient.getAllOthers(userId);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<Object> getById(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                          @PathVariable Long requestId) {
        log.info("GET /requests/{}, user id={}", requestId, userId);
        return requestClient.getById(userId, requestId);
    }
}
