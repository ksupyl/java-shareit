package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;

import java.util.List;

public interface BookingService {

    BookingDto create(Long bookerId, NewBookingDto bookingDto);

    BookingDto approve(Long ownerId, Long bookingId, boolean approved);

    BookingDto getById(Long userId, Long bookingId);

    List<BookingDto> getAllByBooker(Long bookerId, BookingState state);

    List<BookingDto> getAllByOwner(Long ownerId, BookingState state);
}
