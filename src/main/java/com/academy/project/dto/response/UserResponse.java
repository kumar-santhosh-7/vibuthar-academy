package com.academy.project.dto.response;

import com.academy.project.entity.user.User;
import com.academy.project.entity.user.UserRole;
import com.academy.project.entity.user.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String ugDegree;
    private String pgDegree;
    private String address;
    private UserRole role;
    private UserStatus status;
    @Builder.Default
    private List<SubscribedCourseResponse> subscribedCourses = Collections.emptyList();

    public static UserResponse fromEntity(User user) {
        return fromEntity(user, Collections.emptyList());
    }

    public static UserResponse fromEntity(User user, List<SubscribedCourseResponse> subscribedCourses) {
        return UserResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .ugDegree(user.getUgDegree())
                .pgDegree(user.getPgDegree())
                .address(user.getAddress())
                .role(user.getRole())
                .status(user.getStatus())
                .subscribedCourses(subscribedCourses != null ? subscribedCourses : Collections.emptyList())
                .build();
    }
}
