package ru.practicum.shareit.request.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.List;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {

    // запросы, которые создал пользователь
    List<ItemRequest> findByRequestor_Id(Long requestorId, Sort sort);

    // запросы всех, кроме него: на них он может ответить своей вещью
    List<ItemRequest> findByRequestor_IdNot(Long requestorId, Sort sort);
}
