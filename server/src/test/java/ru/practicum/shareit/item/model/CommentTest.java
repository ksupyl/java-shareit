package ru.practicum.shareit.item.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTest {

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        Comment comment = Comment.builder().id(1L).text("Отличная дрель").build();
        Comment sameComment = Comment.builder().id(1L).text("Другой текст").build();

        assertThat(comment).isEqualTo(sameComment);
        assertThat(comment).hasSameHashCodeAs(sameComment);
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
        assertThat(Comment.builder().id(1L).build()).isNotEqualTo(Comment.builder().id(2L).build());
    }

    @Test
    void shouldEqualOnlyItselfWithoutId() {
        Comment newComment = Comment.builder().text("Отличная дрель").build();

        assertThat(newComment).isEqualTo(newComment);
        assertThat(newComment).isNotEqualTo(Comment.builder().text("Отличная дрель").build());
        assertThat(newComment).isNotEqualTo(null);
        assertThat(newComment).isNotEqualTo("Отличная дрель");
    }
}
