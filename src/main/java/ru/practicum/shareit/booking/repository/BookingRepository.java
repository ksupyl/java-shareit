package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.booking.model.Booking;

import java.util.Optional;

/**
 * Вещь и арендатор нужны в каждом ответе про бронь, поэтому @EntityGraph достаёт их тем же запросом.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Override
    @EntityGraph(attributePaths = {"item", "booker"})
    Optional<Booking> findById(Long id);
}
