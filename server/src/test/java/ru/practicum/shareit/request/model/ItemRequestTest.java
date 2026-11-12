package ru.practicum.shareit.request.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Равенство сущности по id: две копии одной записи равны, новая (без id) равна только самой себе.
 */
class ItemRequestTest {

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        ItemRequest request = ItemRequest.builder().id(1L).description("Нужна дрель").build();
        ItemRequest sameRequest = ItemRequest.builder().id(1L).description("Другое описание").build();

        assertThat(request).isEqualTo(sameRequest);
        assertThat(request).hasSameHashCodeAs(sameRequest);
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
        assertThat(ItemRequest.builder().id(1L).build()).isNotEqualTo(ItemRequest.builder().id(2L).build());
    }

    @Test
    void shouldEqualOnlyItselfWithoutId() {
        ItemRequest newRequest = ItemRequest.builder().description("Нужна дрель").build();

        assertThat(newRequest).isEqualTo(newRequest);
        assertThat(newRequest).isNotEqualTo(ItemRequest.builder().description("Нужна дрель").build());
        assertThat(newRequest).isNotEqualTo(null);
        assertThat(newRequest).isNotEqualTo("Нужна дрель");
    }
}
