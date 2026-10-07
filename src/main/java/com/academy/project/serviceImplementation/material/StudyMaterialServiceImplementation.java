package com.academy.project.serviceImplementation.material;

import com.academy.project.dto.material.PresignedUrlResponse;
import com.academy.project.dto.material.StudyMaterialResponse;
import com.academy.project.dto.material.UpdateStudyMaterialRequest;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.entity.material.StudyMaterial;
import com.academy.project.enums.SubscriptionStatus;
import com.academy.project.exception.ApiException;
import com.academy.project.repository.course.CourseRepository;
import com.academy.project.repository.material.StudyMaterialRepository;
import com.academy.project.repository.subscription.CourseSubscriptionRepository;
import com.academy.project.security.SecurityUtils;
import com.academy.project.service.material.StudyMaterialService;
import com.academy.project.service.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudyMaterialServiceImplementation implements StudyMaterialService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private final StudyMaterialRepository studyMaterialRepository;
    private final CourseRepository courseRepository;
    private final CourseSubscriptionRepository courseSubscriptionRepository;
    private final S3StorageService s3StorageService;

    @Value("${app.aws.s3.key-prefix:study-materials}")
    private String keyPrefix;

    @Override
    @Transactional
    public StudyMaterialResponse upload(
            String courseId,
            String title,
            String description,
            MultipartFile file) {
        requireAdmin();
        validateCourse(courseId);
        validateFile(file);

        String trimmedTitle = requireTitle(title);
        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document"
        );
        String extension = extractExtension(originalFileName, file.getContentType());
        String objectKey = buildObjectKey(courseId, extension);

        s3StorageService.upload(objectKey, file);

        StudyMaterial material = StudyMaterial.builder()
                .courseId(courseId.trim())
                .title(trimmedTitle)
                .description(trimToNull(description))
                .originalFileName(originalFileName)
                .s3Key(objectKey)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(currentUserIdOrNull())
                .build();

        return StudyMaterialResponse.fromEntity(studyMaterialRepository.save(material));
    }

    @Override
    @Transactional
    public StudyMaterialResponse update(Long materialId, UpdateStudyMaterialRequest request) {
        requireAdmin();
        StudyMaterial material = findMaterial(materialId);
        material.setTitle(requireTitle(request.getTitle()));
        material.setDescription(trimToNull(request.getDescription()));
        return StudyMaterialResponse.fromEntity(studyMaterialRepository.save(material));
    }

    @Override
    @Transactional
    public void delete(Long materialId) {
        requireAdmin();
        StudyMaterial material = findMaterial(materialId);
        String s3Key = material.getS3Key();
        studyMaterialRepository.delete(material);
        s3StorageService.deleteQuietly(s3Key);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyMaterialResponse getForAdmin(Long materialId) {
        requireAdmin();
        StudyMaterial material = findMaterial(materialId);
        return withFreshDownloadUrl(material);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StudyMaterialResponse> listForAdmin(
            String courseId, String search, int page, int size) {
        requireAdmin();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<StudyMaterial> result;
        boolean hasCourse = courseId != null && !courseId.isBlank();
        boolean hasSearch = search != null && !search.isBlank();

        if (hasCourse && hasSearch) {
            result = studyMaterialRepository.findByCourseIdAndTitleContainingIgnoreCase(
                    courseId.trim(), search.trim(), pageable);
        } else if (hasCourse) {
            result = studyMaterialRepository.findByCourseId(courseId.trim(), pageable);
        } else if (hasSearch) {
            result = studyMaterialRepository.findByTitleContainingIgnoreCase(search.trim(), pageable);
        } else {
            result = studyMaterialRepository.findAll(pageable);
        }

        return PagedResponse.from(result.map(StudyMaterialResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public StudyMaterialResponse getForUser(Long materialId) {
        StudyMaterial material = findMaterial(materialId);
        requireMaterialAccess(material.getCourseId());
        return withFreshDownloadUrl(material);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StudyMaterialResponse> listForUser(String courseId, int page, int size) {
        String userId = requireAuthenticatedUserId();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        if (courseId != null && !courseId.isBlank()) {
            requireCourseSubscription(userId, courseId.trim());
            Page<StudyMaterial> result =
                    studyMaterialRepository.findByCourseId(courseId.trim(), pageable);
            return PagedResponse.from(result.map(StudyMaterialResponse::fromEntity));
        }

        var activeCourseIds = courseSubscriptionRepository.findActiveSubscriptionsForUser(
                        userId, SubscriptionStatus.ACTIVE, LocalDateTime.now()
                ).stream()
                .map(cs -> cs.getCourseId())
                .distinct()
                .toList();

        if (activeCourseIds.isEmpty()) {
            return PagedResponse.<StudyMaterialResponse>builder()
                    .items(java.util.List.of())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .totalItems(0)
                    .totalPages(0)
                    .build();
        }

        Page<StudyMaterial> result =
                studyMaterialRepository.findByCourseIdIn(activeCourseIds, pageable);
        return PagedResponse.from(result.map(StudyMaterialResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public PresignedUrlResponse getDownloadUrl(Long materialId) {
        StudyMaterial material = findMaterial(materialId);
        requireMaterialAccess(material.getCourseId());

        String url = s3StorageService.createPresignedGetUrl(material.getS3Key());
        return PresignedUrlResponse.builder()
                .materialId(material.getId())
                .fileName(material.getOriginalFileName())
                .contentType(material.getContentType())
                .downloadUrl(url)
                .expiresInSeconds(s3StorageService.getPresignDurationSeconds())
                .build();
    }

    private StudyMaterialResponse withFreshDownloadUrl(StudyMaterial material) {
        String url = s3StorageService.createPresignedGetUrl(material.getS3Key());
        return StudyMaterialResponse.withDownloadUrl(
                material, url, s3StorageService.getPresignDurationSeconds());
    }

    private StudyMaterial findMaterial(Long materialId) {
        return studyMaterialRepository.findById(materialId)
                .orElseThrow(() -> ApiException.notFound("Study material not found"));
    }

    private void validateCourse(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw ApiException.badRequest("courseId is required");
        }
        courseRepository.findByCourseId(courseId.trim())
                .orElseThrow(() -> ApiException.notFound("Course not found"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Please upload a study material file");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest(
                    "Allowed file types: PDF, Word, PowerPoint, Excel, TXT, JPEG, PNG"
            );
        }
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("Title is required");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 200) {
            throw ApiException.badRequest("Title must be at most 200 characters");
        }
        return trimmed;
    }

    private String buildObjectKey(String courseId, String extension) {
        String prefix = keyPrefix.endsWith("/")
                ? keyPrefix.substring(0, keyPrefix.length() - 1)
                : keyPrefix;
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + "/" + courseId.trim() + "/" + uuid + extension;
    }

    private String extractExtension(String originalFileName, String contentType) {
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalFileName.length() - 1) {
            return originalFileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        if (contentType == null) {
            return ".bin";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "application/pdf" -> ".pdf";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.ms-powerpoint" -> ".ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "application/vnd.ms-excel" -> ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "text/plain" -> ".txt";
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            default -> ".bin";
        };
    }

    private void requireMaterialAccess(String courseId) {
        if (isAdmin()) {
            return;
        }
        String userId = requireAuthenticatedUserId();
        requireCourseSubscription(userId, courseId);
    }

    private void requireCourseSubscription(String userId, String courseId) {
        boolean subscribed = courseSubscriptionRepository
                .findActiveSubscriptionsForUser(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now())
                .stream()
                .anyMatch(cs -> Objects.equals(cs.getCourseId(), courseId));
        if (!subscribed) {
            throw ApiException.forbidden("Active subscription required for this course");
        }
    }

    private void requireAdmin() {
        if (!isAdmin()) {
            throw ApiException.forbidden("Admin access required");
        }
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
    }

    private String requireAuthenticatedUserId() {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }
        return userId;
    }

    private String currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return SecurityUtils.resolveUserId(auth);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
