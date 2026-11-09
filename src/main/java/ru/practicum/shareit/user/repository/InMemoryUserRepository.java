package ru.practicum.shareit.user.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.user.model.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryUserRepository implements UserRepository {

    // LinkedHashMap помнит порядок добавления, поэтому findAll() отдаёт пользователей по возрастанию id
    private final Map<Long, User> users = new LinkedHashMap<>();
    private long lastId;

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(++lastId);
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public boolean existsByEmail(String email) {
        // Перебором, без отдельного индекса: email меняется при обновлении, и индекс пришлось бы
        // держать в синхроне. В базе это станет уникальным ограничением.
        return users.values().stream()
                .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }

    @Override
    public void deleteById(Long id) {
        users.remove(id);
    }
}
