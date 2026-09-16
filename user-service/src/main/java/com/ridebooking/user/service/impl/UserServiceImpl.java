package com.ridebooking.user.service.impl;

import com.ridebooking.user.dto.CreateUserRequest;
import com.ridebooking.user.dto.UpdateUserRequest;
import com.ridebooking.user.dto.UserResponse;
import com.ridebooking.user.entity.User;
import com.ridebooking.user.exception.ForbiddenException;
import com.ridebooking.user.repository.UserRepository;
import com.ridebooking.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole())
                .phone(request.getPhone())
                .profileImage(request.getProfileImage())
                .build();

        user = userRepository.save(user);
        return mapToResponse(user);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request, String userId, String userRole) {
        Long authId = parseUserId(userId);
        if (!authId.equals(id) && !"ADMIN".equalsIgnoreCase(userRole)) {
            throw new ForbiddenException("You can only update your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getProfileImage() != null) {
            user.setProfileImage(request.getProfileImage());
        }

        user = userRepository.save(user);
        return mapToResponse(user);
    }

    @Override
    public void deleteUser(Long id, String userId, String userRole) {
        Long authId = parseUserId(userId);
        if (!authId.equals(id) && !"ADMIN".equalsIgnoreCase(userRole)) {
            throw new ForbiddenException("You can only delete your own profile");
        }

        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new ForbiddenException("Invalid user identity");
        }
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .build();
    }
}
