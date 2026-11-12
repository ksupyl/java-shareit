package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldCreateUserWithFreeEmail() {
        when(userRepository.existsByEmailIgnoreCase("ksenia@mail.ru")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserDto result = userService.create(UserDto.builder().name("Ksenia").email("ksenia@mail.ru").build());

        assertThat(result).isEqualTo(new UserDto(1L, "Ksenia", "ksenia@mail.ru"));
    }

    @Test
    void shouldNotCreateUserWithTakenEmail() {
        when(userRepository.existsByEmailIgnoreCase("ksenia@mail.ru")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(UserDto.builder().name("Ksenia").email("ksenia@mail.ru").build()))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldUpdateOnlySentFields() {
        User user = User.builder().id(1L).name("Ksenia").email("ksenia@mail.ru").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserDto result = userService.update(1L, UserDto.builder().name("Ксения").build());

        assertThat(result).isEqualTo(new UserDto(1L, "Ксения", "ksenia@mail.ru"));
        // email не присылали — значит, и проверять его занятость незачем
        verify(userRepository, never()).existsByEmailIgnoreCase(anyString());
    }

    @Test
    void shouldNotCheckOwnEmailAsTaken() {
        User user = User.builder().id(1L).name("Ksenia").email("ksenia@mail.ru").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserDto result = userService.update(1L, UserDto.builder().email("KSENIA@mail.ru").build());

        assertThat(result.getEmail()).isEqualTo("KSENIA@mail.ru");
        verify(userRepository, never()).existsByEmailIgnoreCase(anyString());
    }

    @Test
    void shouldNotUpdateEmailToTakenOne() {
        User user = User.builder().id(1L).name("Ksenia").email("ksenia@mail.ru").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("owner@mail.ru")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, UserDto.builder().email("owner@mail.ru").build()))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatedUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, UserDto.builder().name("Ksenia").build()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id=99 not found");
    }

    @Test
    void shouldReturnUserById() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).name("Ksenia").email("ksenia@mail.ru").build()));

        assertThat(userService.getById(1L)).isEqualTo(new UserDto(1L, "Ksenia", "ksenia@mail.ru"));
    }

    @Test
    void shouldReturnAllUsersSortedById() {
        when(userRepository.findAll(Sort.by("id"))).thenReturn(List.of(
                User.builder().id(1L).name("Ksenia").email("ksenia@mail.ru").build(),
                User.builder().id(2L).name("Owner").email("owner@mail.ru").build()));

        assertThat(userService.getAll()).extracting(UserDto::getId).containsExactly(1L, 2L);
    }

    @Test
    void shouldDeleteExistingUser() {
        User user = User.builder().id(1L).name("Ksenia").email("ksenia@mail.ru").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void shouldNotDeleteUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L)).isInstanceOf(NotFoundException.class);
        verify(userRepository, never()).delete(any());
    }
}
