package com.example.taskapprovalsystem.service;

import com.example.taskapprovalsystem.dto.UserDTO;
import com.example.taskapprovalsystem.entity.User;
import com.example.taskapprovalsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserDTO userDTO;
    private User user;

    @BeforeEach
    void setUp() {
        userDTO = new UserDTO();
        userDTO.setName("Test User");
        userDTO.setEmail("test@example.com");
        userDTO.setPassword("password123");

        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
    }

    @Test
    void createUser_Success() {
        when(passwordEncoder.encode(userDTO.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User createdUser = userService.createUser(userDTO);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isEqualTo(1L);
        assertThat(createdUser.getName()).isEqualTo(userDTO.getName());
        assertThat(createdUser.getEmail()).isEqualTo(userDTO.getEmail());

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithNullUserDTO_ThrowsException() {
        assertThatThrownBy(() -> userService.createUser(null))
                .isInstanceOf(Exception.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenRepositoryThrowsException() {
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> userService.createUser(userDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }

    @Test
    void getAllUsers_Success() {
        List<User> expectedUsers = Arrays.asList(
                user,
                createUser(2L, "User 2", "user2@example.com")
        );
        when(userRepository.findAll()).thenReturn(expectedUsers);

        List<User> actualUsers = userService.getAllUsers();

        assertThat(actualUsers).hasSize(2);
        assertThat(actualUsers.get(0).getEmail()).isEqualTo("test@example.com");
        assertThat(actualUsers.get(1).getEmail()).isEqualTo("user2@example.com");

        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_WhenRepositoryThrowsException() {
        when(userRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> userService.getAllUsers())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }

    @Test
    void getAllUsers_ReturnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());
        List<User> users = userService.getAllUsers();
        assertThat(users).isEmpty();
        verify(userRepository).findAll();
    }

    private User createUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encodedPassword");
        return user;
    }
}
