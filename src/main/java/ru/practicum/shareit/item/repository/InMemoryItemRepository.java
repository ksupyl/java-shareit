package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class InMemoryItemRepository implements ItemRepository {

    private final Map<Long, Item> items = new LinkedHashMap<>();
    // владелец у вещи не меняется, поэтому этот индекс не устаревает
    private final Map<Long, Set<Long>> itemIdsByOwnerId = new HashMap<>();
    private long lastId;

    @Override
    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(++lastId);
            itemIdsByOwnerId.computeIfAbsent(item.getOwner().getId(), ownerId -> new LinkedHashSet<>())
                    .add(item.getId());
        }
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<Item> findAllByOwnerId(Long ownerId) {
        return itemIdsByOwnerId.getOrDefault(ownerId, Set.of()).stream()
                .map(items::get)
                .toList();
    }

    @Override
    public List<Item> searchAvailable(String text) {
        String query = text.toLowerCase();
        return items.values().stream()
                .filter(Item::isAvailable)
                .filter(item -> item.getName().toLowerCase().contains(query)
                        || item.getDescription().toLowerCase().contains(query))
                .toList();
    }

    @Override
    public void deleteAllByOwnerId(Long ownerId) {
        Set<Long> itemIds = itemIdsByOwnerId.remove(ownerId);
        if (itemIds != null) {
            itemIds.forEach(items::remove);
        }
    }
}
