package com.academy.project.serviceImplementation.user;

import com.academy.project.dto.response.UserResponse;
import com.academy.project.dto.user.UpdateUserRequest;
import com.academy.project.entity.test.TestAttempt;
import com.academy.project.entity.user.User;
import com.academy.project.entity.user.UserRole;
import com.academy.project.exception.ApiException;
import com.academy.project.repository.payment.CoursePaymentRepository;
import com.academy.project.repository.subscription.CourseSubscriptionRepository;
import com.academy.project.repository.test.AttemptAnswerRepository;
import com.academy.project.repository.test.TestAttemptRepository;
import com.academy.project.repository.user.UserRepository;
import com.academy.project.repository.user.UserSessionRepository;
import com.academy.project.repository.video.VideoWatchProgressRepository;
import com.academy.project.security.SecurityUtils;
import com.academy.project.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final CourseSubscriptionRepository courseSubscriptionRepository;
    private final CoursePaymentRepository coursePaymentRepository;
    private final VideoWatchProgressRepository videoWatchProgressRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        String userId = requireAuthenticatedUserId();

        User user = userRepository.findByUserId(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        requireCanUpdate(userId);

        User user = userRepository.findByUserId(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        String email = normalizeEmail(request.getEmail());
        if (email != null) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
                if (!Objects.equals(existing.getId(), user.getId())) {
                    throw ApiException.conflict("An account with this email already exists");
                }
            });
        }

        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setUgDegree(trimToNull(request.getUgDegree()));
        user.setPgDegree(trimToNull(request.getPgDegree()));
        user.setAddress(trimToNull(request.getAddress()));

        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        String currentUserId = requireAuthenticatedUserId();

        if (Objects.equals(currentUserId, userId)) {
            throw ApiException.badRequest("You cannot delete your own account");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (user.getRole() == UserRole.ADMIN) {
            throw ApiException.forbidden("Admin accounts cannot be deleted");
        }

        List<TestAttempt> attempts = testAttemptRepository.findByUserId(userId);
        if (!attempts.isEmpty()) {
            attemptAnswerRepository.deleteByAttemptIdIn(
                    attempts.stream().map(TestAttempt::getId).toList()
            );
            testAttemptRepository.deleteByUserId(userId);
        }

        videoWatchProgressRepository.deleteByUserId(userId);
        coursePaymentRepository.deleteByUserId(userId);
        courseSubscriptionRepository.deleteByUserId(userId);
        userSessionRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    private void requireCanUpdate(String targetUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = requireAuthenticatedUserId();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        if (!isAdmin && !Objects.equals(currentUserId, targetUserId)) {
            throw ApiException.forbidden("You can only update your own profile");
        }
    }

    private String requireAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = SecurityUtils.resolveUserId(auth);

        if (currentUserId == null) {
            throw ApiException.unauthorized("Login required");
        }

        return currentUserId;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
