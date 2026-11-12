package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private BookingService bookingService;

    private final LocalDateTime start = LocalDateTime.of(2030, 1, 10, 10, 0);
    private final LocalDateTime end = LocalDateTime.of(2030, 1, 11, 10, 0);

    @Test
    void shouldCreateBooking() throws Exception {
        NewBookingDto newBooking = new NewBookingDto(10L, start, end);
        when(bookingService.create(2L, newBooking)).thenReturn(booking(BookingStatus.WAITING));

        mvc.perform(post("/bookings")
                        .header(ShareItHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(newBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.start").value("2030-01-10T10:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-11T10:00:00"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.booker.id").value(2))
                .andExpect(jsonPath("$.item.name").value("Дрель"));
    }

    @Test
    void shouldApproveBooking() throws Exception {
        when(bookingService.approve(1L, 100L, true)).thenReturn(booking(BookingStatus.APPROVED));

        mvc.perform(patch("/bookings/100").header(ShareItHeaders.USER_ID, 1L).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldAnswer403WhenStrangerApproves() throws Exception {
        when(bookingService.approve(3L, 100L, true))
                .thenThrow(new ForbiddenException("Only the item owner can approve booking id=100"));

        mvc.perform(patch("/bookings/100").header(ShareItHeaders.USER_ID, 3L).param("approved", "true"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only the item owner can approve booking id=100"));
    }

    @Test
    void shouldReturnBookingById() throws Exception {
        when(bookingService.getById(2L, 100L)).thenReturn(booking(BookingStatus.WAITING));

        mvc.perform(get("/bookings/100").header(ShareItHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void shouldAnswer404WhenBookingNotFound() throws Exception {
        when(bookingService.getById(2L, 99L)).thenThrow(new NotFoundException("Booking with id=99 not found"));

        mvc.perform(get("/bookings/99").header(ShareItHeaders.USER_ID, 2L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Booking with id=99 not found"));
    }

    @Test
    void shouldListAllBookerBookingsByDefault() throws Exception {
        when(bookingService.getAllByBooker(2L, BookingState.ALL)).thenReturn(List.of(booking(BookingStatus.WAITING)));

        mvc.perform(get("/bookings").header(ShareItHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void shouldPassStateToService() throws Exception {
        when(bookingService.getAllByBooker(2L, BookingState.FUTURE)).thenReturn(List.of());
        when(bookingService.getAllByOwner(1L, BookingState.REJECTED)).thenReturn(List.of());

        mvc.perform(get("/bookings").header(ShareItHeaders.USER_ID, 2L).param("state", "FUTURE"))
                .andExpect(status().isOk());
        mvc.perform(get("/bookings/owner").header(ShareItHeaders.USER_ID, 1L).param("state", "REJECTED"))
                .andExpect(status().isOk());

        verify(bookingService).getAllByBooker(2L, BookingState.FUTURE);
        verify(bookingService).getAllByOwner(1L, BookingState.REJECTED);
    }

    private BookingDto booking(BookingStatus status) {
        return BookingDto.builder()
                .id(100L)
                .start(start)
                .end(end)
                .status(status)
                .booker(new UserDto(2L, "Booker", "booker@mail.ru"))
                .item(new ItemDto(10L, "Дрель", "Ударная", true, null))
                .build();
    }
}
