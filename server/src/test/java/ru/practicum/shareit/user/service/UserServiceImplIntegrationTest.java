package ru.practicum.shareit.user.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserServiceImplIntegrationTest {

    private final EntityManager em;
    private final UserService userService;

    @Test
    void shouldSaveUserToDatabase() {
        UserDto created = userService.create(UserDto.builder().name("Ksenia").email("ksenia@mail.ru").build());

        User saved = em.createQuery("select u from User u where u.email = :email", User.class)
                .setParameter("email", "ksenia@mail.ru")
                .getSingleResult();
        assertThat(saved.getId()).isEqualTo(created.getId());
        assertThat(saved.getName()).isEqualTo("Ksenia");
    }

    @Test
    void shouldCheckEmailIgnoringCase() {
        userService.create(UserDto.builder().name("Ksenia").email("ksenia@mail.ru").build());

        assertThatThrownBy(() -> userService.create(UserDto.builder().name("Copy").email("KSENIA@MAIL.RU").build()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldUpdateUserInDatabase() {
        UserDto created = userService.create(UserDto.builder().name("Ksenia").email("ksenia@mail.ru").build());

        userService.update(created.getId(), UserDto.builder().email("new@mail.ru").build());

        User saved = em.find(User.class, created.getId());
        assertThat(saved.getEmail()).isEqualTo("new@mail.ru");
        assertThat(saved.getName()).isEqualTo("Ksenia");
    }

    @Test
    void shouldReturnAllUsers() {
        UserDto first = userService.create(UserDto.builder().name("Ksenia").email("ksenia@mail.ru").build());
        UserDto second = userService.create(UserDto.builder().name("Owner").email("owner@mail.ru").build());

        List<UserDto> result = userService.getAll();

        assertThat(result).contains(first, second);
    }

    @Test
    void shouldDeleteUserTogetherWithItems() {
        UserDto owner = userService.create(UserDto.builder().name("Owner").email("owner@mail.ru").build());
        Item item = Item.builder()
                .name("Дрель")
                .description("Ударная")
                .available(true)
                .owner(em.find(User.class, owner.getId()))
                .build();
        em.persist(item);
        em.flush();

        userService.delete(owner.getId());
        // отправляем удаление в базу и забываем закешированные сущности, чтобы читать из базы
        em.flush();
        em.clear();

        assertThat(em.find(User.class, owner.getId())).isNull();
        assertThat(em.find(Item.class, item.getId())).isNull();
    }
}
