package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByOwner_IdOrderById(Long ownerId);

    /**
     * Доступные вещи, в названии или описании которых есть этот текст, без учёта регистра.
     */
    @Query("""
            select i from Item i
            where i.available = true
              and (upper(i.name) like upper(concat('%', ?1, '%'))
                or upper(i.description) like upper(concat('%', ?1, '%')))
            order by i.id
            """)
    List<Item> searchAvailable(String text);
}
