package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gateway пропускает к серверу только корректные запросы о пользователях.
 * Клиент сервера заменён моком: проверяем, позвал его контроллер или отказал сам.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private UserClient userClient;

    @Test
    void shouldPassValidUserToServer() throws Exception {
        UserDto user = new UserDto("Ksenia", "ksenia@mail.ru");
        when(userClient.create(user)).thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", 1, "name", "Ksenia", "email", "ksenia@mail.ru")));

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ksenia@mail.ru"));
        verify(userClient).create(user);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"email\":\"ksenia@mail.ru\"}",
            "{\"name\":\" \",\"email\":\"ksenia@mail.ru\"}",
            "{\"name\":\"Ksenia\"}",
            "{\"name\":\"Ksenia\",\"email\":\"ksenia.mail.ru\"}"
    })
    void shouldRejectInvalidNewUser(String body) throws Exception {
        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
        verifyNoInteractions(userClient);
    }

    @Test
    void shouldPassPartialUpdateToServer() throws Exception {
        UserDto update = new UserDto("New name", null);
        when(userClient.update(1L, update)).thenReturn(ResponseEntity.ok(Map.of("id", 1, "name", "New name")));

        mvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));
        verify(userClient).update(1L, update);
    }

    @Test
    void shouldRejectBlankNameOnUpdate() throws Exception {
        mvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("name: must not be blank"));
        verifyNoInteractions(userClient);
    }

    @Test
    void shouldReturnServerAnswersForReadAndDelete() throws Exception {
        when(userClient.getById(1L)).thenReturn(ResponseEntity.ok(Map.of("id", 1)));
        when(userClient.getAll()).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));
        when(userClient.delete(1L)).thenReturn(ResponseEntity.noContent().build());

        mvc.perform(get("/users/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/users")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(delete("/users/1")).andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectNotNumericId() throws Exception {
        mvc.perform(get("/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value 'abc' for parameter 'userId'"));
        verifyNoInteractions(userClient);
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request body is missing or malformed"));
        verifyNoInteractions(userClient);
    }

    @Test
    void shouldAnswer503WhenServerIsUnavailable() throws Exception {
        when(userClient.getAll()).thenThrow(new ResourceAccessException("Connection refused"));

        mvc.perform(get("/users"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("ShareIt server is unavailable"));
    }

    @Test
    void shouldAnswer500OnUnexpectedError() throws Exception {
        when(userClient.getAll()).thenThrow(new IllegalStateException("bug"));

        mvc.perform(get("/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }
}
