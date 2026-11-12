package ru.practicum.shareit.request.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Сервис запросов вместе с настоящей базой H2: проверяем, что запросы и ответы на них
 * сохраняются и читаются так, как нужно API. Каждый тест в своей транзакции, которая потом откатывается.
 * Чужие данные (пользователей, вещи) кладём в базу через EntityManager, а не через другие сервисы:
 * так тест проверяет только сервис запросов и не зависит от остального кода.
 */
@Transactional
@SpringBootTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ItemRequestServiceImplIntegrationTest {

    private final EntityManager em;
    private final ItemRequestService requestService;

    @Test
    void shouldSaveRequestToDatabase() {
        User author = persistUser("Ksenia", "ksenia@mail.ru");

        ItemRequestDto created = requestService.create(author.getId(), new NewItemRequestDto("Нужна дрель"));

        ItemRequest saved = em.createQuery("select r from ItemRequest r where r.id = :id", ItemRequest.class)
                .setParameter("id", created.getId())
                .getSingleResult();
        assertThat(saved.getDescription()).isEqualTo("Нужна дрель");
        assertThat(saved.getRequestor().getId()).isEqualTo(author.getId());
        assertThat(saved.getCreated()).isNotNull();
    }

    @Test
    void shouldReturnOwnRequestsNewestFirstWithAnswers() {
        User author = persistUser("Ksenia", "ksenia@mail.ru");
        User owner = persistUser("Owner", "owner@mail.ru");
        LocalDateTime now = LocalDateTime.now();
        ItemRequest older = persistRequest(author, "Нужна дрель", now.minusDays(2));
        persistRequest(author, "Нужна стремянка", now.minusDays(1));
        persistRequest(owner, "Нужен велосипед", now);
        Item drill = persistItem(owner, "Дрель", older);

        List<ItemRequestDto> result = requestService.getOwn(author.getId());

        assertThat(result).extracting(ItemRequestDto::getDescription)
                .containsExactly("Нужна стремянка", "Нужна дрель");
        assertThat(result.get(0).getItems()).isEmpty();
        assertThat(result.get(1).getItems()).containsExactly(new ItemShortDto(drill.getId(), "Дрель", owner.getId()));
    }

    @Test
    void shouldReturnOnlyRequestsOfOtherUsers() {
        User author = persistUser("Ksenia", "ksenia@mail.ru");
        User other = persistUser("Other", "other@mail.ru");
        LocalDateTime now = LocalDateTime.now();
        persistRequest(author, "Свой запрос", now);
        persistRequest(other, "Чужой старый", now.minusHours(2));
        persistRequest(other, "Чужой новый", now.minusHours(1));

        List<ItemRequestDto> result = requestService.getAllOthers(author.getId());

        assertThat(result).extracting(ItemRequestDto::getDescription)
                .containsExactly("Чужой новый", "Чужой старый");
    }

    @Test
    void shouldReturnRequestByIdWithAnswers() {
        User author = persistUser("Ksenia", "ksenia@mail.ru");
        User owner = persistUser("Owner", "owner@mail.ru");
        ItemRequest request = persistRequest(author, "Нужна дрель", LocalDateTime.now());
        persistItem(owner, "Дрель", request);

        ItemRequestDto result = requestService.getById(owner.getId(), request.getId());

        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getItems()).extracting(ItemShortDto::getName).containsExactly("Дрель");
    }

    private User persistUser(String name, String email) {
        User user = User.builder().name(name).email(email).build();
        em.persist(user);
        return user;
    }

    private ItemRequest persistRequest(User author, String description, LocalDateTime created) {
        ItemRequest request = ItemRequest.builder()
                .description(description)
                .requestor(author)
                .created(created)
                .build();
        em.persist(request);
        return request;
    }

    // вещь, которую хозяин добавил в ответ на запрос
    private Item persistItem(User owner, String name, ItemRequest request) {
        Item item = Item.builder()
                .name(name)
                .description(name + " в хорошем состоянии")
                .available(true)
                .owner(owner)
                .request(request)
                .build();
        em.persist(item);
        return item;
    }
}
