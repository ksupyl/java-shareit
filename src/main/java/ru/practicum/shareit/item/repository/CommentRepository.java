package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.item.model.Comment;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // отзывы сразу нескольких вещей, старые сверху; автора берём тем же запросом — его имя идёт в ответ
    @EntityGraph(attributePaths = "author")
    List<Comment> findByItem_IdInOrderByCreated(Collection<Long> itemIds);
}
