package ru.practicum.shareit.request.dto;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.item.dto.ItemShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON запросов вещей: формат даты создания и список ответов.
 */
@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ItemRequestDtoJsonTest {

    private final JacksonTester<ItemRequestDto> json;
    private final JacksonTester<NewItemRequestDto> newRequestJson;

    @Test
    void shouldWriteRequestWithCreatedToTheSecond() throws Exception {
        ItemRequestDto dto = ItemRequestDto.builder()
                .id(1L)
                .description("Нужна дрель")
                .created(LocalDateTime.of(2026, 11, 12, 10, 30, 15, 123_456_789))
                .items(List.of(new ItemShortDto(5L, "Дрель", 2L)))
                .build();

        JsonContent<ItemRequestDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Нужна дрель");
        // доли секунды в ответ не попадают
        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2026-11-12T10:30:15");
        assertThat(result).extractingJsonPathNumberValue("$.items[0].id").isEqualTo(5);
        assertThat(result).extractingJsonPathStringValue("$.items[0].name").isEqualTo("Дрель");
        assertThat(result).extractingJsonPathNumberValue("$.items[0].ownerId").isEqualTo(2);
    }

    @Test
    void shouldWriteRequestWithoutAnswersAsEmptyArray() throws Exception {
        ItemRequestDto dto = ItemRequestDto.builder()
                .id(1L)
                .description("Нужна дрель")
                .created(LocalDateTime.of(2026, 11, 12, 10, 30))
                .items(List.of())
                .build();

        JsonContent<ItemRequestDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2026-11-12T10:30:00");
        assertThat(result).extractingJsonPathArrayValue("$.items").isEmpty();
    }

    @Test
    void shouldReadNewRequestFromGateway() throws Exception {
        NewItemRequestDto result = newRequestJson.parseObject("{\"description\":\"Нужна дрель\"}");

        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
    }
}
