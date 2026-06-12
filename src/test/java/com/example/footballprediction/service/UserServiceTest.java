package com.example.footballprediction.service;

import com.example.footballprediction.domain.Role;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void changesCurrentUsersPasswordWhenCurrentPasswordMatches() {
        User user = userWithPassword("old-password");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        userService.changePasswordForCurrentUser("USER@example.com", "old-password", "new-password");

        assertThat(passwordEncoder.matches("new-password", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("old-password", user.getPasswordHash())).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void rejectsWrongCurrentPasswordWithoutSaving() {
        User user = userWithPassword("old-password");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePasswordForCurrentUser(
                "user@example.com",
                "wrong-password",
                "new-password"
        ))
                .isInstanceOf(ChangePasswordException.class)
                .hasMessage("Current password is incorrect.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsNewPasswordMatchingStoredCurrentPasswordWithoutSaving() {
        User user = userWithPassword("old-password");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePasswordForCurrentUser(
                "user@example.com",
                "old-password",
                "old-password"
        ))
                .isInstanceOf(ChangePasswordException.class)
                .hasMessage("New password must not be the same as the current password.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsShortNewPasswordWithoutSaving() {
        User user = userWithPassword("old-password");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePasswordForCurrentUser(
                "user@example.com",
                "old-password",
                "short"
        ))
                .isInstanceOf(ChangePasswordException.class)
                .hasMessage("New password should be at least 8 characters.");

        verify(userRepository, never()).save(any(User.class));
    }

    private User userWithPassword(String password) {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("User");
        user.setRole(Role.USER);
        user.setPasswordHash(passwordEncoder.encode(password));
        return user;
    }
}
