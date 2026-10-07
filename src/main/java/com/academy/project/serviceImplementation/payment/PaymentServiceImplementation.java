package com.academy.project.serviceImplementation.payment;

import com.academy.project.dto.payment.CreatePaymentOrderRequest;
import com.academy.project.dto.payment.CreatePaymentOrderResponse;
import com.academy.project.dto.payment.PaymentStatusResponse;
import com.academy.project.dto.payment.VerifyPaymentRequest;
import com.academy.project.dto.subscription.SubscriptionResponse;
import com.academy.project.entity.course.Course;
import com.academy.project.entity.payment.CoursePayment;
import com.academy.project.entity.subscription.CourseSubscription;
import com.academy.project.entity.user.User;
import com.academy.project.entity.user.UserRole;
import com.academy.project.enums.PaymentOrderStatus;
import com.academy.project.enums.PaymentStatus;
import com.academy.project.enums.PaymentType;
import com.academy.project.enums.SubscriptionStatus;
import com.academy.project.exception.ApiException;
import com.academy.project.repository.course.CourseRepository;
import com.academy.project.repository.payment.CoursePaymentRepository;
import com.academy.project.repository.subscription.CourseSubscriptionRepository;
import com.academy.project.repository.user.UserRepository;
import com.academy.project.security.SecurityUtils;
import com.academy.project.service.payment.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImplementation implements PaymentService {

    private final CoursePaymentRepository coursePaymentRepository;
    private final CourseSubscriptionRepository courseSubscriptionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Value("${app.razorpay.key-id:}")
    private String keyId;

    @Value("${app.razorpay.key-secret:}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret:}")
    private String webhookSecret;

    @Override
    @Transactional
    public CreatePaymentOrderResponse createOrder(CreatePaymentOrderRequest request) {
        User student = requireCurrentStudent();
        Course course = courseRepository.findByCourseId(request.getCourseId().trim())
                .orElseThrow(() -> ApiException.notFound("Course not found"));

        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("This course has no payable price. Contact admin for enrollment.");
        }

        ensureNotAlreadySubscribed(student.getUserId(), course.getCourseId());

        BigDecimal amount = course.getPrice().setScale(2, RoundingMode.HALF_UP);
        long amountPaise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
        String receipt = "rcpt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1);

            JSONObject notes = new JSONObject();
            notes.put("userId", student.getUserId());
            notes.put("courseId", course.getCourseId());
            orderRequest.put("notes", notes);

            Order order = razorpayClient().orders.create(orderRequest);
            String razorpayOrderId = order.get("id");

            CoursePayment payment = CoursePayment.builder()
                    .userId(student.getUserId())
                    .courseId(course.getCourseId())
                    .amount(amount)
                    .amountPaise(amountPaise)
                    .currency("INR")
                    .razorpayOrderId(razorpayOrderId)
                    .status(PaymentOrderStatus.CREATED)
                    .receipt(receipt)
                    .build();

            CoursePayment saved = coursePaymentRepository.save(payment);
            log.info("Created Razorpay order {} for user {} course {}",
                    razorpayOrderId, student.getUserId(), course.getCourseId());

            return CreatePaymentOrderResponse.builder()
                    .paymentId(saved.getId())
                    .keyId(keyId.trim())
                    .orderId(razorpayOrderId)
                    .amount(amount)
                    .amountPaise(amountPaise)
                    .currency("INR")
                    .courseId(course.getCourseId())
                    .courseTitle(course.getTitle())
                    .studentName(student.getName())
                    .studentEmail(student.getEmail())
                    .studentPhone(student.getPhone())
                    .receipt(receipt)
                    .build();
        } catch (RazorpayException ex) {
            log.error("Razorpay order creation failed: {}", ex.toString());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to create payment order. Try again.");
        }
    }

    @Override
    @Transactional
    public SubscriptionResponse verifyPayment(VerifyPaymentRequest request) {
        requireConfiguredKeys();

        User student = requireCurrentStudent();
        CoursePayment payment = coursePaymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId().trim())
                .orElseThrow(() -> ApiException.notFound("Payment order not found"));

        if (!Objects.equals(payment.getUserId(), student.getUserId())) {
            throw ApiException.forbidden("This payment does not belong to you");
        }

        if (payment.getStatus() == PaymentOrderStatus.PAID && payment.getSubscriptionId() != null) {
            return loadSubscriptionResponse(payment.getSubscriptionId());
        }

        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId().trim());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId().trim());
            attributes.put("razorpay_signature", request.getRazorpaySignature().trim());

            boolean valid = Utils.verifyPaymentSignature(attributes, keySecret.trim());
            if (!valid) {
                payment.setStatus(PaymentOrderStatus.FAILED);
                coursePaymentRepository.save(payment);
                throw ApiException.badRequest("Invalid payment signature");
            }
        } catch (RazorpayException ex) {
            log.error("Payment signature verification failed: {}", ex.toString());
            throw ApiException.badRequest("Payment verification failed");
        }

        CourseSubscription subscription = activatePaidSubscription(
                payment,
                request.getRazorpayPaymentId().trim(),
                null
        );

        Course course = courseRepository.findByCourseId(payment.getCourseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));
        return SubscriptionResponse.from(subscription, student, course);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(String orderId) {
        User student = requireAuthenticatedUser();
        CoursePayment payment = coursePaymentRepository.findByRazorpayOrderId(orderId.trim())
                .orElseThrow(() -> ApiException.notFound("Payment order not found"));

        boolean isAdmin = isAdmin();
        if (!isAdmin && !Objects.equals(payment.getUserId(), student.getUserId())) {
            throw ApiException.forbidden("This payment does not belong to you");
        }

        return toStatusResponse(payment);
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Webhook secret is not configured");
        }
        if (signature == null || signature.isBlank()) {
            throw ApiException.unauthorized("Missing Razorpay signature");
        }

        try {
            boolean valid = Utils.verifyWebhookSignature(payload, signature, webhookSecret.trim());
            if (!valid) {
                throw ApiException.unauthorized("Invalid webhook signature");
            }
        } catch (RazorpayException ex) {
            log.error("Webhook signature verification error: {}", ex.toString());
            throw ApiException.unauthorized("Invalid webhook signature");
        }

        try {
            JSONObject root = new JSONObject(payload);
            String event = root.optString("event", "");

            if (!"payment.captured".equals(event) && !"order.paid".equals(event)) {
                log.info("Ignoring Razorpay webhook event: {}", event);
                return;
            }

            JSONObject payloadNode = root.optJSONObject("payload");
            if (payloadNode == null) {
                log.warn("Webhook missing payload for event {}", event);
                return;
            }

            JSONObject paymentWrapper = payloadNode.optJSONObject("payment");
            JSONObject paymentEntity = paymentWrapper != null ? paymentWrapper.optJSONObject("entity") : null;
            if (paymentEntity == null) {
                if ("order.paid".equals(event)) {
                    log.info("order.paid received without payment entity; waiting for payment.captured or /verify");
                    return;
                }
                log.warn("Webhook payload missing payment entity for event {}", event);
                return;
            }

            String orderId = paymentEntity.optString("order_id", null);
            String paymentId = paymentEntity.optString("id", null);
            String method = paymentEntity.optString("method", null);
            String status = paymentEntity.optString("status", null);

            if (orderId == null || paymentId == null || orderId.isBlank() || paymentId.isBlank()) {
                log.warn("Webhook missing order_id or payment id");
                return;
            }

            if (status != null && !status.isBlank()
                    && !"captured".equalsIgnoreCase(status)
                    && !"authorized".equalsIgnoreCase(status)) {
                log.info("Ignoring payment status {} for {}", status, paymentId);
                return;
            }

            CoursePayment payment = coursePaymentRepository.findByRazorpayOrderId(orderId).orElse(null);
            if (payment == null) {
                log.warn("No local payment found for Razorpay order {}", orderId);
                return;
            }

            if (payment.getStatus() == PaymentOrderStatus.PAID) {
                log.info("Payment already processed for order {}", orderId);
                return;
            }

            activatePaidSubscription(payment, paymentId, method);
            log.info("Webhook activated subscription for order {} payment {}", orderId, paymentId);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process Razorpay webhook: {}", ex.toString());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process webhook");
        }
    }

    private CourseSubscription activatePaidSubscription(
            CoursePayment payment,
            String razorpayPaymentId,
            String paymentMethod) {
        if (payment.getStatus() == PaymentOrderStatus.PAID && payment.getSubscriptionId() != null) {
            return courseSubscriptionRepository.findById(payment.getSubscriptionId())
                    .orElseThrow(() -> ApiException.notFound("Subscription not found"));
        }

        coursePaymentRepository.findByRazorpayPaymentId(razorpayPaymentId).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), payment.getId())
                    && existing.getStatus() == PaymentOrderStatus.PAID) {
                throw ApiException.conflict("This Razorpay payment was already used");
            }
        });

        try {
            ensureNotAlreadySubscribed(payment.getUserId(), payment.getCourseId());
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.CONFLICT) {
                CoursePayment refreshed = coursePaymentRepository.findById(payment.getId()).orElse(payment);
                if (refreshed.getSubscriptionId() != null) {
                    return courseSubscriptionRepository.findById(refreshed.getSubscriptionId())
                            .orElseThrow(() -> ApiException.notFound("Subscription not found"));
                }
                CourseSubscription existing = courseSubscriptionRepository
                        .findActiveSubscriptionsForUser(
                                payment.getUserId(), SubscriptionStatus.ACTIVE, LocalDateTime.now())
                        .stream()
                        .filter(sub -> Objects.equals(sub.getCourseId(), payment.getCourseId()))
                        .findFirst()
                        .orElseThrow(() -> ex);

                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setStatus(PaymentOrderStatus.PAID);
                payment.setPaymentMethod(paymentMethod);
                payment.setSubscriptionId(existing.getId());
                coursePaymentRepository.save(payment);
                return existing;
            }
            throw ex;
        }

        User student = userRepository.findByUserId(payment.getUserId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("Student not found"));

        Course course = courseRepository.findByCourseId(payment.getCourseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));

        LocalDateTime now = LocalDateTime.now();
        CourseSubscription subscription = CourseSubscription.builder()
                .userId(student.getUserId())
                .courseId(course.getCourseId())
                .status(SubscriptionStatus.ACTIVE)
                .paymentType(mapPaymentType(paymentMethod))
                .paymentStatus(PaymentStatus.PAID)
                .paidAmount(payment.getAmount())
                .subscribedAt(now)
                .expiresAt(null)
                .build();

        CourseSubscription savedSubscription = courseSubscriptionRepository.save(subscription);

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentOrderStatus.PAID);
        payment.setPaymentMethod(paymentMethod);
        payment.setSubscriptionId(savedSubscription.getId());
        coursePaymentRepository.save(payment);

        return savedSubscription;
    }

    private PaymentType mapPaymentType(String method) {
        if (method == null || method.isBlank()) {
            return PaymentType.ONLINE;
        }
        return switch (method.toLowerCase()) {
            case "upi" -> PaymentType.UPI;
            case "card" -> PaymentType.CARD;
            case "netbanking" -> PaymentType.BANK_TRANSFER;
            default -> PaymentType.ONLINE;
        };
    }

    private void ensureNotAlreadySubscribed(String userId, String courseId) {
        boolean alreadySubscribed = courseSubscriptionRepository
                .findActiveSubscriptionsForUser(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now())
                .stream()
                .anyMatch(sub -> Objects.equals(sub.getCourseId(), courseId));
        if (alreadySubscribed) {
            throw ApiException.conflict("Student is already subscribed to this course");
        }
    }

    private SubscriptionResponse loadSubscriptionResponse(Long subscriptionId) {
        CourseSubscription subscription = courseSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ApiException.notFound("Subscription not found"));
        User student = userRepository.findByUserId(subscription.getUserId())
                .orElseThrow(() -> ApiException.notFound("Student not found"));
        Course course = courseRepository.findByCourseId(subscription.getCourseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));
        return SubscriptionResponse.from(subscription, student, course);
    }

    private PaymentStatusResponse toStatusResponse(CoursePayment payment) {
        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .courseId(payment.getCourseId())
                .orderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .subscriptionId(payment.getSubscriptionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private RazorpayClient razorpayClient() throws RazorpayException {
        requireConfiguredKeys();
        return new RazorpayClient(keyId.trim(), keySecret.trim());
    }

    private void requireConfiguredKeys() {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
    }

    private User requireCurrentStudent() {
        User user = requireAuthenticatedUser();
        if (user.getRole() != UserRole.STUDENT && !isAdmin()) {
            throw ApiException.forbidden("Only students can pay for course subscriptions");
        }
        if (user.getRole() == UserRole.STUDENT && !user.isActive()) {
            throw ApiException.badRequest("Student account is not active");
        }
        return user;
    }

    private User requireAuthenticatedUser() {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }
        return userRepository.findByUserId(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private String currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return SecurityUtils.resolveUserId(auth);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
    }
}
