package com.academy.project.serviceImplementation.subscription;

import com.academy.project.dto.subscription.CreateSubscriptionRequest;
import com.academy.project.dto.subscription.PaidAmountResponse;
import com.academy.project.dto.subscription.SubscriptionResponse;
import com.academy.project.dto.subscription.SubscriptionStatsResponse;
import com.academy.project.dto.subscription.UpdateSubscriptionPaymentRequest;
import com.academy.project.entity.course.Course;
import com.academy.project.entity.subscription.CourseSubscription;
import com.academy.project.entity.user.User;
import com.academy.project.entity.user.UserRole;
import com.academy.project.enums.PaymentStatus;
import com.academy.project.enums.SubscriptionStatus;
import com.academy.project.exception.ApiException;
import com.academy.project.repository.course.CourseRepository;
import com.academy.project.repository.subscription.CourseSubscriptionRepository;
import com.academy.project.repository.user.UserRepository;
import com.academy.project.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImplementation implements SubscriptionService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseSubscriptionRepository courseSubscriptionRepository;

    @Override
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        User student = userRepository.findByUserId(request.getStudentId())
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("Student not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw ApiException.badRequest("Subscriptions can only be added for students");
        }

        if (!student.isActive()) {
            throw ApiException.badRequest("Student account is not active");
        }

        Course course = courseRepository.findByCourseId(request.getCourseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));

        LocalDateTime now = LocalDateTime.now();
        List<CourseSubscription> activeSubscriptions = courseSubscriptionRepository
                .findActiveSubscriptionsForUser(student.getUserId(), SubscriptionStatus.ACTIVE, now);

        boolean alreadySubscribed = activeSubscriptions.stream()
                .anyMatch(sub -> sub.getCourseId().equals(course.getCourseId()));

        if (alreadySubscribed) {
            throw ApiException.conflict("Student is already subscribed to this course");
        }

        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(now)) {
            throw ApiException.badRequest("Expiry date must be in the future");
        }

        ResolvedPayment payment = resolvePayment(request.getAmount(), request.getPaymentStatus(), course.getPrice());

        CourseSubscription subscription = CourseSubscription.builder()
                .userId(student.getUserId())
                .courseId(course.getCourseId())
                .status(SubscriptionStatus.ACTIVE)
                .paymentType(request.getPaymentType())
                .paymentStatus(payment.status())
                .paidAmount(payment.amount())
                .subscribedAt(now)
                .expiresAt(request.getExpiresAt())
                .build();

        CourseSubscription saved = courseSubscriptionRepository.save(subscription);
        return SubscriptionResponse.from(saved, student, course);
    }

    @Override
    @Transactional
    public SubscriptionResponse updateSubscriptionPayment(
            Long subscriptionId,
            UpdateSubscriptionPaymentRequest request) {
        CourseSubscription subscription = courseSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ApiException.notFound("Subscription not found"));

        User student = userRepository.findByUserId(request.getStudentId())
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("Student not found"));

        if (!subscription.getUserId().equals(student.getUserId())) {
            throw ApiException.badRequest("Student id does not match this subscription");
        }

        Course course = courseRepository.findByCourseId(subscription.getCourseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));

        if (request.getPaymentType() == null
                && request.getPaymentStatus() == null
                && request.getAmount() == null) {
            throw ApiException.badRequest("Provide payment type, payment status, or amount to update");
        }

        BigDecimal amountToResolve = request.getAmount();
        if (amountToResolve == null && request.getPaymentStatus() == null) {
            amountToResolve = subscription.getPaidAmount();
        }

        ResolvedPayment payment = resolvePayment(
                amountToResolve,
                request.getPaymentStatus(),
                course.getPrice()
        );

        if (request.getPaymentType() != null) {
            subscription.setPaymentType(request.getPaymentType());
        }
        subscription.setPaymentStatus(payment.status());
        subscription.setPaidAmount(payment.amount());

        CourseSubscription saved = courseSubscriptionRepository.save(subscription);
        return SubscriptionResponse.from(saved, student, course);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatsResponse getSubscriptionStats() {
        LocalDateTime now = LocalDateTime.now();
        long subscribedMemberCount = courseSubscriptionRepository.countActiveSubscribers(
                SubscriptionStatus.ACTIVE, now
        );
        long totalStudents = userRepository.countByRoleAndDeletedAtIsNull(UserRole.STUDENT);
        long unsubscribedMemberCount = Math.max(0, totalStudents - subscribedMemberCount);
        long courseCount = courseRepository.count();

        return SubscriptionStatsResponse.builder()
                .subscribedMemberCount(subscribedMemberCount)
                .unsubscribedMemberCount(unsubscribedMemberCount)
                .courseCount(courseCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaidAmountResponse getTotalPaidAmount(String courseId) {
        String normalizedCourseId = (courseId == null || courseId.isBlank()) ? null : courseId.trim();

        if (normalizedCourseId != null) {
            courseRepository.findByCourseId(normalizedCourseId)
                    .orElseThrow(() -> ApiException.notFound("Course not found"));
        }

        BigDecimal totalPaidAmount = courseSubscriptionRepository.sumPaidAmount(normalizedCourseId);
        if (totalPaidAmount == null) {
            totalPaidAmount = BigDecimal.ZERO;
        }

        return PaidAmountResponse.builder()
                .totalPaidAmount(totalPaidAmount)
                .build();
    }

    private ResolvedPayment resolvePayment(
            BigDecimal requestedAmount,
            PaymentStatus requestedStatus,
            BigDecimal coursePrice) {
        if (requestedAmount != null && requestedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("Paid amount cannot be negative");
        }
        if (requestedAmount != null && coursePrice != null && requestedAmount.compareTo(coursePrice) > 0) {
            throw ApiException.badRequest("Paid amount cannot exceed course price");
        }

        BigDecimal amount = requestedAmount;
        if (amount == null) {
            amount = switch (requestedStatus == null ? PaymentStatus.NOT_PAID : requestedStatus) {
                case NOT_PAID -> BigDecimal.ZERO;
                case PAID -> {
                    if (coursePrice == null) {
                        throw ApiException.badRequest("Paid amount is required when the course has no price");
                    }
                    yield coursePrice;
                }
                case PARTIAL -> throw ApiException.badRequest("Paid amount is required for partial payment");
            };
        }

        PaymentStatus derived = derivePaymentStatus(amount, coursePrice);
        if (requestedStatus != null && requestedStatus != derived) {
            throw ApiException.badRequest("Payment status does not match the paid amount and course price");
        }

        return new ResolvedPayment(amount, derived);
    }

    private PaymentStatus derivePaymentStatus(BigDecimal amount, BigDecimal coursePrice) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return PaymentStatus.NOT_PAID;
        }
        if (coursePrice != null && amount.compareTo(coursePrice) >= 0) {
            return PaymentStatus.PAID;
        }
        if (coursePrice == null) {
            return PaymentStatus.PAID;
        }
        return PaymentStatus.PARTIAL;
    }

    private record ResolvedPayment(BigDecimal amount, PaymentStatus status) {
    }
}
