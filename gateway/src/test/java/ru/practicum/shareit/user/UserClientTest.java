package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.user.dto.UserDto;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Клиент gateway и BaseClient под ним: куда уходит запрос и что gateway отдаёт клиенту в ответ.
 * Вместо сервера ShareIt — MockRestServiceServer, он отвечает так, как мы ему скажем.
 */
@RestClientTest(UserClient.class)
class UserClientTest {

    @Autowired
    private UserClient userClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void shouldSendUserToServerAndReturnItsAnswer() {
        server.expect(requestTo("http://localhost:9090/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"name\":\"Ksenia\",\"email\":\"ksenia@mail.ru\"}"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":1,\"name\":\"Ksenia\",\"email\":\"ksenia@mail.ru\"}"));

        ResponseEntity<Object> response = userClient.create(new UserDto("Ksenia", "ksenia@mail.ru"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(Map.of("id", 1, "name", "Ksenia", "email", "ksenia@mail.ru"));
        server.verify();
    }

    @Test
    void shouldPassServerErrorToClientAsJson() {
        server.expect(requestTo("http://localhost:9090/users/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"User with id=99 not found\"}"));

        ResponseEntity<Object> response = userClient.getById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"error\":\"User with id=99 not found\"}");
    }

    @Test
    void shouldNotCopyServiceHeadersOfServer() {
        server.expect(requestTo("http://localhost:9090/users"))
                .andRespond(withSuccess("[{\"id\":1}]", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.TRANSFER_ENCODING, "chunked"));

        ResponseEntity<Object> response = userClient.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(List.of(Map.of("id", 1)));
        assertThat(response.getHeaders()).isEmpty();
    }

    @Test
    void shouldUpdateAndDeleteUser() {
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("{\"name\":\"New name\"}"))
                .andRespond(withSuccess("{\"id\":1,\"name\":\"New name\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThat(userClient.update(1L, new UserDto("New name", null)).getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<Object> deleted = userClient.delete(1L);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(deleted.getBody()).isNull();
        server.verify();
    }
}
