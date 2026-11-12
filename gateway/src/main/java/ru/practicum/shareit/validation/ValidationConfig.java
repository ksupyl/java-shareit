package ru.practicum.shareit.validation;

import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ValidationConfig {

    // Даты в API приходят с точностью до секунды, например 2026-11-12T10:00:00, поэтому и «сейчас»
    // для @Future и @FutureOrPresent берём без долей секунды. Иначе бронь, которая начинается
    // в текущую секунду, к приходу запроса уже оказалась бы «в прошлом».
    @Bean
    public ValidationConfigurationCustomizer secondPrecisionClock() {
        return configuration -> configuration.clockProvider(() -> Clock.tickSeconds(ZoneId.systemDefault()));
    }
}
