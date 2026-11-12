package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
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
    private ItemService itemService;

    private final ItemDto itemDto = new ItemDto(10L, "Дрель", "Ударная", true, 3L);

    @Test
    void shouldCreateItem() throws Exception {
        ItemDto newItem = ItemDto.builder().name("Дрель").description("Ударная").available(true).requestId(3L).build();
        when(itemService.create(1L, newItem)).thenReturn(itemDto);

        mvc.perform(post("/items")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.requestId").value(3));
    }

    @Test
    void shouldUpdateItem() throws Exception {
        ItemDto update = ItemDto.builder().available(false).build();
        when(itemService.update(1L, 10L, update)).thenReturn(new ItemDto(10L, "Дрель", "Ударная", false, null));

        mvc.perform(patch("/items/10")
                        .header(ShareItHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void shouldAnswer403WhenStrangerUpdatesItem() throws Exception {
        ItemDto update = ItemDto.builder().name("Чужая").build();
        when(itemService.update(2L, 10L, update)).thenThrow(new ForbiddenException("Only the owner can edit item id=10"));

        mvc.perform(patch("/items/10")
                        .header(ShareItHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(update)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only the owner can edit item id=10"));
    }

    @Test
    void shouldReturnItemCardWithBookingsAndComments() throws Exception {
        ItemWithBookingsDto card = ItemWithBookingsDto.builder()
                .id(10L).name("Дрель").description("Ударная").available(true)
                .lastBooking(new BookingShortDto(100L, 2L,
                        LocalDateTime.of(2026, 11, 1, 10, 0), LocalDateTime.of(2026, 11, 2, 10, 0)))
                .comments(List.of(new CommentDto(7L, "Отличная дрель", "Booker",
                        LocalDateTime.of(2026, 11, 3, 12, 0))))
                .build();
        when(itemService.getById(1L, 10L)).thenReturn(card);

        mvc.perform(get("/items/10").header(ShareItHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastBooking.bookerId").value(2))
                .andExpect(jsonPath("$.lastBooking.start").value("2026-11-01T10:00:00"))
                .andExpect(jsonPath("$.nextBooking").isEmpty())
                .andExpect(jsonPath("$.comments[0].authorName").value("Booker"));
    }

    @Test
    void shouldShowItemWithoutUserHeader() throws Exception {
        when(itemService.getById(null, 10L)).thenReturn(ItemWithBookingsDto.builder().id(10L).build());

        mvc.perform(get("/items/10"))
                .andExpect(status().isOk());
        verify(itemService).getById(null, 10L);
    }

    @Test
    void shouldReturnOwnerItemsAndSearchResults() throws Exception {
        when(itemService.getAllByOwner(1L)).thenReturn(List.of(ItemWithBookingsDto.builder().id(10L).build()));
        when(itemService.search("дрель")).thenReturn(List.of(itemDto));

        mvc.perform(get("/items").header(ShareItHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
        mvc.perform(get("/items/search").param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Дрель"));
    }

    @Test
    void shouldAddComment() throws Exception {
        CommentDto newComment = CommentDto.builder().text("Отличная дрель").build();
        when(itemService.addComment(2L, 10L, newComment)).thenReturn(new CommentDto(7L, "Отличная дрель", "Booker",
                LocalDateTime.of(2026, 11, 3, 12, 0, 30, 999_000_000)));

        mvc.perform(post("/items/10/comment")
                        .header(ShareItHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(newComment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Booker"))
                .andExpect(jsonPath("$.created").value("2026-11-03T12:00:30"));
    }

    @Test
    void shouldAnswer400WhenCommentIsNotAllowed() throws Exception {
        CommentDto newComment = CommentDto.builder().text("Хочу").build();
        when(itemService.addComment(2L, 10L, newComment))
                .thenThrow(new ValidationException("User id=2 has no finished booking of item id=10"));

        mvc.perform(post("/items/10/comment")
                        .header(ShareItHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(newComment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User id=2 has no finished booking of item id=10"));
    }
}
