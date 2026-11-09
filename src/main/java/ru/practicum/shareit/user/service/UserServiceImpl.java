package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    public UserDto create(UserDto userDto) {
        checkEmailIsFree(userDto.getEmail());
        User user = userRepository.save(UserMapper.toUser(userDto));
        log.info("Created user id={}", user.getId());
        return UserMapper.toUserDto(user);
    }

    @Override
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
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    public void delete(Long userId) {
        User user = getUserOrThrow(userId);
        itemRepository.deleteAllByOwnerId(user.getId());
        userRepository.deleteById(user.getId());
        log.info("Deleted user id={} and their items", userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
    }

    private void checkEmailIsFree(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " is already in use");
        }
    }
}
