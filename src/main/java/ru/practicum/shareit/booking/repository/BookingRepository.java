package ru.practicum.shareit.booking.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Вещь и арендатор нужны в каждом ответе про бронь, поэтому @EntityGraph достаёт их тем же запросом.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Override
    @EntityGraph(attributePaths = {"item", "booker"})
    Optional<Booking> findById(Long id);

    // брони, которые сделал пользователь

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBooker_Id(Long bookerId, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBooker_IdAndStartIsBeforeAndEndIsAfter(Long bookerId, LocalDateTime start,
                                                               LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBooker_IdAndEndIsBefore(Long bookerId, LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBooker_IdAndStartIsAfter(Long bookerId, LocalDateTime start, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBooker_IdAndStatus(Long bookerId, BookingStatus status, Sort sort);

    // брони на вещи пользователя: те же выборки, только через владельца вещи

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItem_Owner_Id(Long ownerId, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(Long ownerId, LocalDateTime start,
                                                                   LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItem_Owner_IdAndEndIsBefore(Long ownerId, LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItem_Owner_IdAndStartIsAfter(Long ownerId, LocalDateTime start, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItem_Owner_IdAndStatus(Long ownerId, BookingStatus status, Sort sort);

    // брони сразу нескольких вещей — для дат в карточках. Здесь нужны только id и даты, поэтому без графа
    List<Booking> findByItem_IdInAndStatus(Collection<Long> itemIds, BookingStatus status);

    // брал ли пользователь вещь и закончилась ли аренда — без этого отзыв не принимаем
    boolean existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(Long itemId, Long bookerId, BookingStatus status,
                                                               LocalDateTime end);
}
