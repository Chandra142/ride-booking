package com.ridebooking.user.service;

import com.ridebooking.user.dto.CreateUserRequest;
import com.ridebooking.user.dto.UpdateUserRequest;
import com.ridebooking.user.dto.UserResponse;
import com.ridebooking.user.entity.User;
import com.ridebooking.user.exception.ForbiddenException;
import com.ridebooking.user.exception.ResourceNotFoundException;
import com.ridebooking.user.repository.UserRepository;
import com.ridebooking.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@test.com")
                .password("password123")
                .role("USER")
                .phone("1234567890")
                .profileImage("http://img.test/photo.jpg")
                .build();
    }

    @Test
    void createUser_shouldReturnUserResponse() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("John Doe");
        request.setEmail("john@test.com");
        request.setPassword("password123");
        request.setRole("USER");
        request.setPhone("1234567890");

        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john@test.com");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getPhone()).isEqualTo("1234567890");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowWhenEmailExists() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("John Doe");
        request.setEmail("john@test.com");
        request.setPassword("password123");
        request.setRole("USER");

        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void getUserById_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAllUsers_shouldReturnList() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserResponse> users = userService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void updateUser_shouldUpdateOwnProfile() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Jane Doe");
        request.setPhone("0987654321");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateUser(1L, request, "1", "USER");

        assertThat(response.getFullName()).isEqualTo("Jane Doe");
        assertThat(response.getPhone()).isEqualTo("0987654321");
        assertThat(response.getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void updateUser_shouldAllowAdminToUpdateOthers() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Admin Updated");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateUser(1L, request, "2", "ADMIN");

        assertThat(response.getFullName()).isEqualTo("Admin Updated");
    }

    @Test
    void updateUser_shouldThrowForForbiddenAccess() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Hacker");

        assertThatThrownBy(() -> userService.updateUser(1L, request, "2", "USER"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only update your own profile");
    }

    @Test
    void updateUser_shouldThrowForInvalidUserId() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Test");

        assertThatThrownBy(() -> userService.updateUser(1L, request, "invalid", "USER"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Invalid user identity");
    }

    @Test
    void updateUser_shouldThrowWhenUserNotFound() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Test");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, request, "99", "USER"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateUser_shouldNotChangeNullFields() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateUser(1L, request, "1", "USER");

        assertThat(response.getFullName()).isEqualTo("Updated Name");
        assertThat(response.getPhone()).isEqualTo("1234567890");
    }

    @Test
    void deleteUser_shouldDeleteOwnProfile() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUser(1L, "1", "USER");

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_shouldAllowAdminToDeleteOthers() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUser(1L, "2", "ADMIN");

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrowForForbiddenAccess() {
        assertThatThrownBy(() -> userService.deleteUser(1L, "2", "USER"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only delete your own profile");
    }

    @Test
    void deleteUser_shouldThrowWhenUserNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L, "99", "USER"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void mapToResponse_shouldNotIncludePassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john@test.com");
        assertThat(response.getPhone()).isEqualTo("1234567890");
    }
}
