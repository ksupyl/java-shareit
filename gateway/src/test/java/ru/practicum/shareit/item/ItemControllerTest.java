package ru.practicum.shareit.item;

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
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ItemClient itemClient;

    @Test
    void shouldPassValidItemToServer() throws Exception {
        ItemDto item = ItemDto.builder().name("Дрель").description("Ударная").available(true).requestId(3L).build();
        when(itemClient.create(1L, item)).thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", 10, "name", "Дрель", "requestId", 3)));

        mvc.perform(post("/items")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.requestId").value(3));
        verify(itemClient).create(1L, item);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"description\":\"Ударная\",\"available\":true}",
            "{\"name\":\"Дрель\",\"available\":true}",
            "{\"name\":\"Дрель\",\"description\":\"Ударная\"}",
            "{\"name\":\"\",\"description\":\"Ударная\",\"available\":true}"
    })
    void shouldRejectItemWithoutRequiredFields(String body) throws Exception {
        mvc.perform(post("/items")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
        verifyNoInteractions(itemClient);
    }

    @Test
    void shouldRejectNonPositiveRequestId() throws Exception {
        ItemDto item = ItemDto.builder().name("Дрель").description("Ударная").available(true).requestId(0L).build();

        mvc.perform(post("/items")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("requestId: must be greater than 0"));
        verifyNoInteractions(itemClient);
    }

    @Test
    void shouldRequireUserHeaderToCreateItem() throws Exception {
        mvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дрель\",\"description\":\"Ударная\",\"available\":true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(itemClient);
    }

    @Test
    void shouldPassPartialUpdateToServer() throws Exception {
        ItemDto update = ItemDto.builder().available(false).build();
        when(itemClient.update(1L, 10L, update)).thenReturn(ResponseEntity.ok(Map.of("id", 10, "available", false)));

        mvc.perform(patch("/items/10")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void shouldRejectBlankDescriptionOnUpdate() throws Exception {
        mvc.perform(patch("/items/10")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("description: must not be blank"));
        verifyNoInteractions(itemClient);
    }

    @Test
    void shouldShowItemWithoutUserHeader() throws Exception {
        when(itemClient.getById(null, 10L)).thenReturn(ResponseEntity.ok(Map.of("id", 10)));

        mvc.perform(get("/items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
        verify(itemClient).getById(null, 10L);
    }

    @Test
    void shouldReturnOwnerItems() throws Exception {
        when(itemClient.getAllByOwner(1L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 10))));

        mvc.perform(get("/items").header(ShareItHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void shouldSearchOnServer() throws Exception {
        when(itemClient.search("дрель")).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 10))));

        mvc.perform(get("/items/search").param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void shouldAnswerBlankSearchWithoutServer() throws Exception {
        mvc.perform(get("/items/search").param("text", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        verifyNoInteractions(itemClient);
    }

    @Test
    void shouldPassValidCommentToServer() throws Exception {
        CommentDto comment = new CommentDto("Отличная дрель");
        when(itemClient.addComment(2L, 10L, comment)).thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", 1, "text", "Отличная дрель")));

        mvc.perform(post("/items/10/comment")
                        .header(ShareItHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(comment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Отличная дрель"));
    }

    @Test
    void shouldRejectBlankComment() throws Exception {
        mvc.perform(post("/items/10/comment")
                        .header(ShareItHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("text: must not be blank"));
        verifyNoInteractions(itemClient);
    }
}
