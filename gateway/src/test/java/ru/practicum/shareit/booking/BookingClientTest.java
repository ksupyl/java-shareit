package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.common.ShareItHeaders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(BookingClient.class)
class BookingClientTest {

    @Autowired
    private BookingClient bookingClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void shouldSendBookingWithUserHeader() {
        server.expect(requestTo("http://localhost:9090/bookings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(ShareItHeaders.USER_ID, "2"))
                .andExpect(content().json(
                        "{\"itemId\":10,\"start\":\"2030-01-10T10:00:00\",\"end\":\"2030-01-11T10:00:00\"}"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":1,\"status\":\"WAITING\"}"));

        NewBookingDto booking = new NewBookingDto(10L,
                LocalDateTime.of(2030, 1, 10, 10, 0), LocalDateTime.of(2030, 1, 11, 10, 0));

        assertThat(bookingClient.create(2L, booking).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        server.verify();
    }

    @Test
    void shouldSendDecisionAsParameter() {
        server.expect(requestTo("http://localhost:9090/bookings/5?approved=false"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(ShareItHeaders.USER_ID, "1"))
                .andRespond(withSuccess("{\"id\":5,\"status\":\"REJECTED\"}", MediaType.APPLICATION_JSON));

        assertThat(bookingClient.approve(1L, 5L, false).getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void shouldAskBookingsWithState() {
        server.expect(requestTo("http://localhost:9090/bookings/7"))
                .andRespond(withSuccess("{\"id\":7}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:9090/bookings?state=FUTURE"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:9090/bookings/owner?state=WAITING"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        bookingClient.getById(2L, 7L);
        bookingClient.getAllByBooker(2L, BookingState.FUTURE);
        bookingClient.getAllByOwner(1L, BookingState.WAITING);

        server.verify();
    }
}
