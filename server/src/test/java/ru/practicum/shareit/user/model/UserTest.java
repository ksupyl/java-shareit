package ru.practicum.shareit.user.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        User user = User.builder().id(1L).name("Ksenia").email("ksenia@mail.ru").build();
        User sameUser = User.builder().id(1L).name("Другое имя").email("other@mail.ru").build();

        assertThat(user).isEqualTo(sameUser);
        assertThat(user).hasSameHashCodeAs(sameUser);
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
        assertThat(User.builder().id(1L).build()).isNotEqualTo(User.builder().id(2L).build());
    }

    @Test
    void shouldEqualOnlyItselfWithoutId() {
        User newUser = User.builder().name("Ksenia").email("ksenia@mail.ru").build();

        assertThat(newUser).isEqualTo(newUser);
        assertThat(newUser).isNotEqualTo(User.builder().name("Ksenia").email("ksenia@mail.ru").build());
        assertThat(newUser).isNotEqualTo(null);
        assertThat(newUser).isNotEqualTo("ksenia@mail.ru");
    }
}
