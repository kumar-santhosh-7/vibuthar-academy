package com.academy.project.repository.payment;

import com.academy.project.entity.payment.CoursePayment;
import com.academy.project.enums.PaymentOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoursePaymentRepository extends JpaRepository<CoursePayment, Long> {

    Optional<CoursePayment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<CoursePayment> findByRazorpayPaymentId(String razorpayPaymentId);

    boolean existsByUserIdAndCourseIdAndStatus(String userId, String courseId, PaymentOrderStatus status);

    void deleteByUserId(String userId);
}
