package com.ridebooking.user.service;

import com.ridebooking.user.dto.CreateUserRequest;
import com.ridebooking.user.dto.UpdateUserRequest;
import com.ridebooking.user.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long id, UpdateUserRequest request, String userId, String userRole);
    void deleteUser(Long id, String userId, String userRole);
}
