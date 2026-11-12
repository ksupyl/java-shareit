package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.request.dto.NewItemRequestDto;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ItemRequestClient requestClient;

    @Test
    void shouldPassValidRequestToServer() throws Exception {
        NewItemRequestDto request = new NewItemRequestDto("Нужна дрель");
        when(requestClient.create(1L, request)).thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", 1, "description", "Нужна дрель")));

        mvc.perform(post("/requests")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Нужна дрель"));
        verify(requestClient).create(1L, request);
    }

    @Test
    void shouldRejectBlankDescription() throws Exception {
        mvc.perform(post("/requests")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("description: must not be blank"));
        verifyNoInteractions(requestClient);
    }

    @Test
    void shouldRejectTooLongDescription() throws Exception {
        NewItemRequestDto request = new NewItemRequestDto("a".repeat(1001));

        mvc.perform(post("/requests")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(requestClient);
    }

    @Test
    void shouldRequireUserHeader() throws Exception {
        mvc.perform(get("/requests"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
        verifyNoInteractions(requestClient);
    }

    @Test
    void shouldReturnServerAnswersForAllReadEndpoints() throws Exception {
        when(requestClient.getOwn(1L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));
        when(requestClient.getAllOthers(1L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 2))));
        when(requestClient.getById(1L, 2L)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mvc.perform(get("/requests").header(ShareItHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/requests/all").header(ShareItHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/requests/2").header(ShareItHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }
}
