package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {

    Item save(Item item);

    Optional<Item> findById(Long id);

    List<Item> findAllByOwnerId(Long ownerId);

    /**
     * Доступные вещи, в названии или описании которых есть этот текст, без учёта регистра.
     */
    List<Item> searchAvailable(String text);
}
