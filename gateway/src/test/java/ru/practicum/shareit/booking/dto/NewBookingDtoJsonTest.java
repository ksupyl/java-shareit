package ru.practicum.shareit.booking.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Бронь, которую gateway принимает от клиента и пересылает серверу: даты в JSON и правила проверки.
 */
@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class NewBookingDtoJsonTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final JacksonTester<NewBookingDto> json;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void shouldReadDatesFromClient() throws Exception {
        String content = "{\"itemId\":10,\"start\":\"2030-01-10T10:00:00\",\"end\":\"2030-01-11T12:30:00\"}";

        NewBookingDto result = json.parseObject(content);

        assertThat(result.getItemId()).isEqualTo(10L);
        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 10, 10, 0));
        assertThat(result.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 11, 12, 30));
    }

    @Test
    void shouldWriteDatesForServerAsStrings() throws Exception {
        NewBookingDto booking = new NewBookingDto(10L,
                LocalDateTime.of(2030, 1, 10, 10, 0), LocalDateTime.of(2030, 1, 11, 12, 30));

        JsonContent<NewBookingDto> result = json.write(booking);

        // Сервер ждёт даты строками ISO. Jackson без настроек записал бы LocalDateTime массивом чисел
        // [2030,1,10,10,0], а Spring Boot настраивает его писать строкой — тест следит, чтобы так и было.
        assertThat(result).extractingJsonPathNumberValue("$.itemId").isEqualTo(10);
        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-10T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-11T12:30:00");
    }

    @Test
    void shouldAcceptBookingInFuture() {
        NewBookingDto booking = new NewBookingDto(10L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        assertThat(validator.validate(booking)).isEmpty();
    }

    @Test
    void shouldRejectEndBeforeStartOnEndField() {
        NewBookingDto booking = new NewBookingDto(10L, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1));

        Set<ConstraintViolation<NewBookingDto>> violations = validator.validate(booking);

        assertThat(violations).hasSize(1);
        ConstraintViolation<NewBookingDto> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("end");
        assertThat(violation.getMessage()).isEqualTo("must be after start");
    }

    @Test
    void shouldRejectEqualStartAndEnd() {
        LocalDateTime moment = LocalDateTime.now().plusDays(1);

        assertThat(validator.validate(new NewBookingDto(10L, moment, moment))).hasSize(1);
    }

    @Test
    void shouldReportMissingDatesOnlyOnce() {
        // без дат сравнивать нечего: ошибки только от @NotNull, без лишней "must be after start"
        Set<ConstraintViolation<NewBookingDto>> violations = validator.validate(new NewBookingDto(10L, null, null));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("start", "end");
    }
}
