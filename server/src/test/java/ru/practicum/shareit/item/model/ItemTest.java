package ru.practicum.shareit.item.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemTest {

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        Item item = Item.builder().id(1L).name("Дрель").build();
        Item sameItem = Item.builder().id(1L).name("Другое название").build();

        assertThat(item).isEqualTo(sameItem);
        assertThat(item).hasSameHashCodeAs(sameItem);
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
        assertThat(Item.builder().id(1L).build()).isNotEqualTo(Item.builder().id(2L).build());
    }

    @Test
    void shouldEqualOnlyItselfWithoutId() {
        Item newItem = Item.builder().name("Дрель").build();

        assertThat(newItem).isEqualTo(newItem);
        assertThat(newItem).isNotEqualTo(Item.builder().name("Дрель").build());
        assertThat(newItem).isNotEqualTo(null);
        assertThat(newItem).isNotEqualTo("Дрель");
    }
}
