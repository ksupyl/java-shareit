package ru.practicum.shareit.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.practicum.shareit.booking.dto.NewBookingDto;

import java.time.LocalDateTime;

public class StartBeforeEndValidator implements ConstraintValidator<StartBeforeEnd, NewBookingDto> {

    @Override
    public boolean isValid(NewBookingDto booking, ConstraintValidatorContext context) {
        LocalDateTime start = booking.getStart();
        LocalDateTime end = booking.getEnd();
        // без одной из дат сравнивать нечего: об этом уже скажет @NotNull на поле
        if (start == null || end == null || end.isAfter(start)) {
            return true;
        }
        // ошибку вешаем на поле end, чтобы клиент получил "end: must be after start"
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("end")
                .addConstraintViolation();
        return false;
    }
}
