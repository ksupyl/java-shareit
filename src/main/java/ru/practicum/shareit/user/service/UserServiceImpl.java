package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        checkEmailIsFree(userDto.getEmail());
        User user = userRepository.save(UserMapper.toUser(userDto));
        log.info("Created user id={}", user.getId());
        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public UserDto update(Long userId, UserDto userDto) {
        User user = getUserOrThrow(userId);
        String newEmail = userDto.getEmail();
        // свой же текущий email — не конфликт
        if (newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail())) {
            checkEmailIsFree(newEmail);
        }
        UserMapper.updateUserFields(user, userDto);
        User updated = userRepository.save(user);
        log.info("Updated user id={}", userId);
        return UserMapper.toUserDto(updated);
    }

    @Override
    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(getUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        User user = getUserOrThrow(userId);
        // вещи, брони и отзывы пользователя база удалит сама — ON DELETE CASCADE в schema.sql
        userRepository.delete(user);
        log.info("Deleted user id={} and their items", userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
    }

    private void checkEmailIsFree(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email " + email + " is already in use");
        }
    }
}
