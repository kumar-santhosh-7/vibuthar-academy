package com.academy.project.repository.subscription;

import com.academy.project.entity.subscription.CourseSubscription;
import com.academy.project.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CourseSubscriptionRepository extends JpaRepository<CourseSubscription, Long> {

    @Query("""
            SELECT cs FROM CourseSubscription cs
            WHERE cs.userId = :userId
              AND cs.status = :status
              AND (cs.expiresAt IS NULL OR cs.expiresAt > :now)
            """)
    List<CourseSubscription> findActiveSubscriptionsForUser(
            @Param("userId") String userId,
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now
    );

    boolean existsByUserIdAndCourseIdAndStatusAndExpiresAtIsNullOrExpiresAtAfter(
            String userId, String courseId, SubscriptionStatus status, LocalDateTime now
    );

    @Query("""
            SELECT DISTINCT cs.userId FROM CourseSubscription cs
            WHERE cs.status = :status
              AND (cs.expiresAt IS NULL OR cs.expiresAt > :now)
              AND (:courseId IS NULL OR cs.courseId = :courseId)
            """)
    List<String> findActiveSubscriberUserIds(
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now,
            @Param("courseId") String courseId
    );

    @Query("""
            SELECT COUNT(DISTINCT cs.userId) FROM CourseSubscription cs
            WHERE cs.status = :status
              AND (cs.expiresAt IS NULL OR cs.expiresAt > :now)
            """)
    long countActiveSubscribers(
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT COALESCE(SUM(cs.paidAmount), 0)
            FROM CourseSubscription cs
            WHERE (:courseId IS NULL OR cs.courseId = :courseId)
            """)
    BigDecimal sumPaidAmount(@Param("courseId") String courseId);
}
