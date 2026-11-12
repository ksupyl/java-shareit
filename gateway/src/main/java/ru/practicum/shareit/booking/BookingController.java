package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.common.ShareItHeaders;

@Slf4j
@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingClient bookingClient;

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                         @RequestBody @Valid NewBookingDto bookingDto) {
        log.info("POST /bookings, user id={}", userId);
        return bookingClient.create(userId, bookingDto);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<Object> approve(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                          @PathVariable Long bookingId,
                                          @RequestParam boolean approved) {
        log.info("PATCH /bookings/{}?approved={}, user id={}", bookingId, approved, userId);
        return bookingClient.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Object> getById(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                          @PathVariable Long bookingId) {
        log.info("GET /bookings/{}, user id={}", bookingId, userId);
        return bookingClient.getById(userId, bookingId);
    }

    @GetMapping
    public ResponseEntity<Object> getAllByBooker(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                                 @RequestParam(defaultValue = "ALL") BookingState state) {
        log.info("GET /bookings?state={}, user id={}", state, userId);
        return bookingClient.getAllByBooker(userId, state);
    }

    @GetMapping("/owner")
    public ResponseEntity<Object> getAllByOwner(@RequestHeader(ShareItHeaders.USER_ID) Long userId,
                                                @RequestParam(defaultValue = "ALL") BookingState state) {
        log.info("GET /bookings/owner?state={}, user id={}", state, userId);
        return bookingClient.getAllByOwner(userId, state);
    }
}
