package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Веб-слой запросов: адреса, заголовок, статусы и JSON. Сервис заменён моком.
 */
@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ItemRequestService requestService;

    private final ItemRequestDto requestDto = ItemRequestDto.builder()
            .id(1L)
            .description("Нужна дрель")
            .created(LocalDateTime.of(2026, 11, 12, 10, 0, 0))
            .items(List.of(new ItemShortDto(5L, "Дрель", 2L)))
            .build();

    @Test
    void shouldCreateRequest() throws Exception {
        NewItemRequestDto newRequest = new NewItemRequestDto("Нужна дрель");
        when(requestService.create(1L, newRequest)).thenReturn(requestDto);

        mvc.perform(post("/requests")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(newRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Нужна дрель"))
                .andExpect(jsonPath("$.created").value("2026-11-12T10:00:00"))
                .andExpect(jsonPath("$.items[0].id").value(5))
                .andExpect(jsonPath("$.items[0].name").value("Дрель"))
                .andExpect(jsonPath("$.items[0].ownerId").value(2));
        verify(requestService).create(1L, newRequest);
    }

    @Test
    void shouldReturnOwnRequests() throws Exception {
        when(requestService.getOwn(1L)).thenReturn(List.of(requestDto));

        mvc.perform(get("/requests").header(ShareItHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Нужна дрель"));
    }

    @Test
    void shouldReturnRequestsOfOtherUsers() throws Exception {
        when(requestService.getAllOthers(2L)).thenReturn(List.of(requestDto));

        mvc.perform(get("/requests/all").header(ShareItHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        verify(requestService).getAllOthers(2L);
    }

    @Test
    void shouldReturnRequestById() throws Exception {
        when(requestService.getById(2L, 1L)).thenReturn(requestDto);

        mvc.perform(get("/requests/1").header(ShareItHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Дрель"));
    }

    @Test
    void shouldAnswer404WhenRequestNotFound() throws Exception {
        when(requestService.getById(1L, 99L)).thenThrow(new NotFoundException("Item request with id=99 not found"));

        mvc.perform(get("/requests/99").header(ShareItHeaders.USER_ID, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Item request with id=99 not found"));
    }
}
