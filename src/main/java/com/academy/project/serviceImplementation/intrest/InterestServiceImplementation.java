package com.academy.project.serviceImplementation.intrest;

import com.academy.project.dto.intrest.InterestRequest;
import com.academy.project.dto.intrest.InterestResponse;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.entity.intrest.Interest;
import com.academy.project.enums.EmailStatus;
import com.academy.project.repository.interest.InterestRepository;
import com.academy.project.service.intrest.InterestService;
// import com.academy.project.service.emailService.IntrestEmailService; // SMTP mail temporarily disabled
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestServiceImplementation implements InterestService {

    private final InterestRepository interestRepository;
    // private final IntrestEmailService emailService; // SMTP mail temporarily disabled

    @Override
    public InterestResponse createInterest(InterestRequest request) {

        // 1. Save candidate in MySQL
        Interest interest = Interest.builder()
                .username(request.getUsername())
                .emailId(request.getEmailId())
                .description(request.getDescription())
                .courseOfInterest(request.getCourseOfInterest())
                .mobileNumber(request.getMobileNumber())
                .emailStatus(EmailStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Interest savedInterest = interestRepository.save(interest);

        // 2. Send email — SMTP mail temporarily disabled
        /*
        try {

            emailService.sendInterestNotification(savedInterest);

            savedInterest.setEmailStatus(EmailStatus.SENT);

        } catch (Exception exception) {

            savedInterest.setEmailStatus(EmailStatus.FAILED);

            // Log the actual error
            System.err.println(
                    "Failed to send interest email: "
                            + exception.getMessage()
            );
            savedInterest.setEmailStatus(EmailStatus.FAILED);
            log.error("Failed to send interest email for interestId={}: {}",
                    savedInterest.getId(), exception.getMessage(), exception);

        }

        // 3. Update email status
        Interest updatedInterest =
                interestRepository.save(savedInterest);

        return InterestResponse.builder()
                .id(updatedInterest.getId())
                .username(updatedInterest.getUsername())
                .emailId(updatedInterest.getEmailId())
                .courseOfInterest(updatedInterest.getCourseOfInterest())
                .emailStatus(updatedInterest.getEmailStatus())
                .createdAt(updatedInterest.getCreatedAt())
                .build();
        */

        return InterestResponse.builder()
                .id(savedInterest.getId())
                .username(savedInterest.getUsername())
                .emailId(savedInterest.getEmailId())
                .courseOfInterest(savedInterest.getCourseOfInterest())
                .emailStatus(savedInterest.getEmailStatus())
                .createdAt(savedInterest.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InterestResponse> listInterests(
            String search,
            String courseOfInterest,
            EmailStatus emailStatus,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Interest> interestPage = interestRepository.searchInterests(
                normalize(search),
                normalize(courseOfInterest),
                emailStatus,
                pageRequest
        );

        Page<InterestResponse> mapped = interestPage.map(this::toResponse);
        return PagedResponse.from(mapped);
    }

    private InterestResponse toResponse(Interest interest) {
        return InterestResponse.builder()
                .id(interest.getId())
                .username(interest.getUsername())
                .emailId(interest.getEmailId())
                .courseOfInterest(interest.getCourseOfInterest())
                .emailStatus(interest.getEmailStatus())
                .createdAt(interest.getCreatedAt())
                .mobileNumber(interest.getMobileNumber())
                .description(interest.getDescription())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
