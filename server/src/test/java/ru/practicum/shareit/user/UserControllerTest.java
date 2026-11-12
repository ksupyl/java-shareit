package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private UserService userService;

    private final UserDto userDto = new UserDto(1L, "Ksenia", "ksenia@mail.ru");

    @Test
    void shouldCreateUser() throws Exception {
        UserDto newUser = UserDto.builder().name("Ksenia").email("ksenia@mail.ru").build();
        when(userService.create(newUser)).thenReturn(userDto);

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ksenia"))
                .andExpect(jsonPath("$.email").value("ksenia@mail.ru"));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UserDto update = UserDto.builder().name("Ксения").build();
        when(userService.update(1L, update)).thenReturn(new UserDto(1L, "Ксения", "ksenia@mail.ru"));

        mvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ксения"));
    }

    @Test
    void shouldReturnUserAndAllUsers() throws Exception {
        when(userService.getById(1L)).thenReturn(userDto);
        when(userService.getAll()).thenReturn(List.of(userDto));

        mvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ksenia@mail.ru"));
        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        mvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());
        verify(userService).delete(1L);
    }

    @Test
    void shouldAnswer404WhenUserNotFound() throws Exception {
        when(userService.getById(99L)).thenThrow(new NotFoundException("User with id=99 not found"));

        mvc.perform(get("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User with id=99 not found"));
    }

    @Test
    void shouldAnswer409WhenEmailIsTaken() throws Exception {
        UserDto newUser = UserDto.builder().name("Copy").email("ksenia@mail.ru").build();
        when(userService.create(newUser)).thenThrow(new ConflictException("Email ksenia@mail.ru is already in use"));

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(newUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email ksenia@mail.ru is already in use"));
    }

    @Test
    void shouldHideDatabaseTextOnConstraintViolation() throws Exception {
        when(userService.getAll()).thenThrow(new DataIntegrityViolationException("uq_user_email violated"));

        mvc.perform(get("/users"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Data conflicts with existing records"));
    }

    @Test
    void shouldAnswer500OnUnexpectedError() throws Exception {
        when(userService.getAll()).thenThrow(new IllegalStateException("bug"));

        mvc.perform(get("/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }
}
