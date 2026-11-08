package ru.practicum.shareit.user.repository;

import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Имена методов — как в Spring Data. Когда перейдём на базу, поменяем реализацию, а сервисы почти не тронем.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    boolean existsByEmail(String email);

    void deleteById(Long id);
}