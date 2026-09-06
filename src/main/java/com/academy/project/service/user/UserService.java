package com.academy.project.service.user;

import com.academy.project.dto.response.UserResponse;
import com.academy.project.dto.user.UpdateUserRequest;

public interface UserService {

    UserResponse getCurrentUserProfile();

    UserResponse updateUser(String userId, UpdateUserRequest request);
}
