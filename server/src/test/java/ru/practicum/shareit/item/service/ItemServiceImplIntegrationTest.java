package ru.practicum.shareit.item.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ItemServiceImplIntegrationTest {

    private final EntityManager em;
    private final ItemService itemService;

    @Test
    void shouldReturnOwnerItemsWithLastAndNextBookingsAndComments() {
        User owner = persistUser("Owner", "owner@mail.ru");
        User booker = persistUser("Booker", "booker@mail.ru");
        Item drill = persistItem(owner, "Дрель", true);
        Item saw = persistItem(owner, "Пила", true);
        persistItem(booker, "Чужая вещь", true);
        LocalDateTime now = LocalDateTime.now();
        Booking last = persistBooking(drill, booker, now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED);
        Booking next = persistBooking(drill, booker, now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED);
        // ожидающие и отклонённые брони в карточку не попадают, даже если они ближе
        persistBooking(drill, booker, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        persistBooking(drill, booker, now.minusHours(5), now.minusHours(4), BookingStatus.REJECTED);
        persistComment(drill, booker, "Отличная дрель", now.minusDays(1));

        List<ItemWithBookingsDto> result = itemService.getAllByOwner(owner.getId());

        assertThat(result).extracting(ItemWithBookingsDto::getName).containsExactly("Дрель", "Пила");
        ItemWithBookingsDto drillCard = result.get(0);
        assertThat(drillCard.getLastBooking().getId()).isEqualTo(last.getId());
        assertThat(drillCard.getNextBooking().getId()).isEqualTo(next.getId());
        assertThat(drillCard.getComments()).extracting(CommentDto::getText).containsExactly("Отличная дрель");
        assertThat(drillCard.getComments().get(0).getAuthorName()).isEqualTo("Booker");
        ItemWithBookingsDto sawCard = result.get(1);
        assertThat(sawCard.getId()).isEqualTo(saw.getId());
        assertThat(sawCard.getLastBooking()).isNull();
        assertThat(sawCard.getNextBooking()).isNull();
        assertThat(sawCard.getComments()).isEmpty();
    }

    @Test
    void shouldSaveItemAsAnswerToRequest() {
        User author = persistUser("Ksenia", "ksenia@mail.ru");
        User owner = persistUser("Owner", "owner@mail.ru");
        ItemRequest request = ItemRequest.builder()
                .description("Нужна дрель")
                .requestor(author)
                .created(LocalDateTime.now())
                .build();
        em.persist(request);

        ItemDto created = itemService.create(owner.getId(), ItemDto.builder()
                .name("Дрель").description("Ударная").available(true).requestId(request.getId()).build());

        Item saved = em.find(Item.class, created.getId());
        assertThat(created.getRequestId()).isEqualTo(request.getId());
        assertThat(saved.getRequest().getId()).isEqualTo(request.getId());
        assertThat(saved.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    void shouldShowItemWithoutBookingsToOtherUser() {
        User owner = persistUser("Owner", "owner@mail.ru");
        User booker = persistUser("Booker", "booker@mail.ru");
        Item drill = persistItem(owner, "Дрель", true);
        LocalDateTime now = LocalDateTime.now();
        persistBooking(drill, booker, now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED);

        ItemWithBookingsDto result = itemService.getById(booker.getId(), drill.getId());

        assertThat(result.getName()).isEqualTo("Дрель");
        assertThat(result.getNextBooking()).isNull();
    }

    @Test
    void shouldFindOnlyAvailableItemsIgnoringCase() {
        User owner = persistUser("Owner", "owner@mail.ru");
        persistItem(owner, "Дрель ударная", true);
        persistItem(owner, "Дрель старая", false);
        persistItem(owner, "Пила", true);

        List<ItemDto> result = itemService.search("ДРЕЛЬ");

        assertThat(result).extracting(ItemDto::getName).containsExactly("Дрель ударная");
    }

    @Test
    void shouldSaveCommentAfterFinishedBooking() {
        User owner = persistUser("Owner", "owner@mail.ru");
        User booker = persistUser("Booker", "booker@mail.ru");
        Item drill = persistItem(owner, "Дрель", true);
        LocalDateTime now = LocalDateTime.now();
        persistBooking(drill, booker, now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED);

        CommentDto created = itemService.addComment(booker.getId(), drill.getId(),
                CommentDto.builder().text("Отличная дрель").build());

        Comment saved = em.find(Comment.class, created.getId());
        assertThat(saved.getText()).isEqualTo("Отличная дрель");
        assertThat(saved.getAuthor().getId()).isEqualTo(booker.getId());
        assertThat(saved.getItem().getId()).isEqualTo(drill.getId());
    }

    private User persistUser(String name, String email) {
        User user = User.builder().name(name).email(email).build();
        em.persist(user);
        return user;
    }

    private Item persistItem(User owner, String name, boolean available) {
        Item item = Item.builder().name(name).description(name + " в хорошем состоянии")
                .available(available).owner(owner).build();
        em.persist(item);
        return item;
    }

    private Booking persistBooking(Item item, User booker, LocalDateTime start, LocalDateTime end,
                                   BookingStatus status) {
        Booking booking = Booking.builder().item(item).booker(booker).start(start).end(end).status(status).build();
        em.persist(booking);
        return booking;
    }

    private void persistComment(Item item, User author, String text, LocalDateTime created) {
        em.persist(Comment.builder().item(item).author(author).text(text).created(created).build());
    }
}
