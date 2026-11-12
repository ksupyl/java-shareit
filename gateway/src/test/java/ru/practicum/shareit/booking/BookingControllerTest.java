package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.common.ShareItHeaders;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    // даты в API — до секунды, поэтому и в тестах отбрасываем доли секунды
    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private BookingClient bookingClient;

    @Test
    void shouldPassValidBookingToServer() throws Exception {
        NewBookingDto booking = new NewBookingDto(10L, NOW.plusDays(1), NOW.plusDays(2));
        when(bookingClient.create(2L, booking)).thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", 1, "status", "WAITING")));

        mvc.perform(post("/bookings")
                        .header(ShareItHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"));
        verify(bookingClient).create(2L, booking);
    }

    static Stream<NewBookingDto> invalidBookings() {
        return Stream.of(
                new NewBookingDto(null, NOW.plusDays(1), NOW.plusDays(2)),   // нет вещи
                new NewBookingDto(0L, NOW.plusDays(1), NOW.plusDays(2)),     // id вещи — ноль
                new NewBookingDto(10L, null, NOW.plusDays(2)),               // нет начала
                new NewBookingDto(10L, NOW.plusDays(1), null),               // нет конца
                new NewBookingDto(10L, NOW.minusDays(1), NOW.plusDays(2)),   // начало в прошлом
                new NewBookingDto(10L, NOW.minusDays(3), NOW.minusDays(2)),  // конец в прошлом
                new NewBookingDto(10L, NOW.plusDays(1), NOW.plusDays(1)),    // начало равно концу
                new NewBookingDto(10L, NOW.plusDays(2), NOW.plusDays(1))     // конец раньше начала
        );
    }

    @ParameterizedTest
    @MethodSource("invalidBookings")
    void shouldRejectInvalidBooking(NewBookingDto booking) throws Exception {
        mvc.perform(post("/bookings")
                        .header(ShareItHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(booking)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
        verifyNoInteractions(bookingClient);
    }

    @Test
    void shouldExplainWhyEndBeforeStartIsRejected() throws Exception {
        NewBookingDto booking = new NewBookingDto(10L, NOW.plusDays(2), NOW.plusDays(1));

        mvc.perform(post("/bookings")
                        .header(ShareItHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(booking)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("end: must be after start"));
    }

    @Test
    void shouldPassApprovalToServer() throws Exception {
        when(bookingClient.approve(1L, 5L, true)).thenReturn(ResponseEntity.ok(Map.of("id", 5, "status", "APPROVED")));

        mvc.perform(patch("/bookings/5").header(ShareItHeaders.USER_ID, 1L).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        verify(bookingClient).approve(1L, 5L, true);
    }

    @Test
    void shouldRejectApprovalWithoutDecision() throws Exception {
        mvc.perform(patch("/bookings/5").header(ShareItHeaders.USER_ID, 1L))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/bookings/5").header(ShareItHeaders.USER_ID, 1L).param("approved", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value 'maybe' for parameter 'approved'"));
        verifyNoInteractions(bookingClient);
    }

    @Test
    void shouldReturnBookingById() throws Exception {
        when(bookingClient.getById(2L, 5L)).thenReturn(ResponseEntity.ok(Map.of("id", 5)));

        mvc.perform(get("/bookings/5").header(ShareItHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void shouldAskForAllBookerBookingsByDefault() throws Exception {
        when(bookingClient.getAllByBooker(2L, BookingState.ALL)).thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/bookings").header(ShareItHeaders.USER_ID, 2L))
                .andExpect(status().isOk());
        verify(bookingClient).getAllByBooker(2L, BookingState.ALL);
    }

    @Test
    void shouldPassStateOfOwnerBookingsToServer() throws Exception {
        when(bookingClient.getAllByOwner(1L, BookingState.WAITING)).thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/bookings/owner").header(ShareItHeaders.USER_ID, 1L).param("state", "WAITING"))
                .andExpect(status().isOk());
        verify(bookingClient).getAllByOwner(1L, BookingState.WAITING);
    }

    @Test
    void shouldRejectUnknownState() throws Exception {
        mvc.perform(get("/bookings").header(ShareItHeaders.USER_ID, 2L).param("state", "SOON"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value 'SOON' for parameter 'state'"));
        verifyNoInteractions(bookingClient);
    }
}
