package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.NewBookingDto;

public interface BookingService {

    BookingDto create(Long bookerId, NewBookingDto bookingDto);

    BookingDto approve(Long ownerId, Long bookingId, boolean approved);
}
