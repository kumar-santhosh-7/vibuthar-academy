package com.academy.project.serviceImplementation.course;

import com.academy.project.dto.course.AddCourseVideoRequest;
import com.academy.project.dto.course.CreateCourseRequest;
import com.academy.project.dto.course.CourseResponse;
import com.academy.project.dto.course.CourseVideoResponse;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.entity.course.Course;
import com.academy.project.entity.course.CourseVideo;
import com.academy.project.enums.CourseStatus;
import com.academy.project.exception.ApiException;
import com.academy.project.entity.material.StudyMaterial;
import com.academy.project.repository.course.CourseRepository;
import com.academy.project.repository.course.CourseVideoRepository;
import com.academy.project.repository.material.StudyMaterialRepository;
import com.academy.project.repository.subscription.CourseSubscriptionRepository;
import com.academy.project.repository.video.VideoWatchProgressRepository;
import com.academy.project.service.course.CourseService;
import com.academy.project.service.storage.S3StorageService;
import com.academy.project.util.CourseIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImplementation implements CourseService {

    private static final String THUMBNAIL_SUBDIR = "thumbnails";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final CourseRepository courseRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final CourseSubscriptionRepository courseSubscriptionRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final VideoWatchProgressRepository videoWatchProgressRepository;
    private final S3StorageService s3StorageService;

    @Value("${app.images.storage-dir:images}")
    private String storageDir;

    @Value("${app.images.url-prefix:/images}")
    private String urlPrefix;

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request, MultipartFile thumbnail) {
        String thumbnailUrl = storeThumbnailIfPresent(thumbnail);

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .durationHours(request.getDurationHours())
                .price(request.getPrice())
                .status(request.getStatus() != null ? request.getStatus() : CourseStatus.ACTIVE)
                .thumbnailUrl(thumbnailUrl)
                .build();

        course = courseRepository.save(course);
        course.setCourseId(CourseIdGenerator.generate(course));
        course = courseRepository.save(course);

        return CourseResponse.fromEntity(course);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CourseResponse> listCourses(CourseStatus status, String search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Course> coursePage;
        if (search != null && !search.isBlank()) {
            if (status != null) {
                coursePage = courseRepository.findByStatusAndTitleContainingIgnoreCase(status, search.trim(), pageRequest);
            } else {
                coursePage = courseRepository.findByTitleContainingIgnoreCase(search.trim(), pageRequest);
            }
        } else if (status != null) {
            coursePage = courseRepository.findByStatus(status, pageRequest);
        } else {
            coursePage = courseRepository.findAll(pageRequest);
        }

        List<Long> coursePks = coursePage.getContent().stream()
                .map(Course::getId)
                .toList();

        Map<Long, List<CourseVideo>> videosByCoursePk = coursePks.isEmpty()
                ? Collections.emptyMap()
                : courseVideoRepository.findByCourseIdInOrderBySortOrderAsc(coursePks).stream()
                .collect(Collectors.groupingBy(CourseVideo::getCourseId));

        Page<CourseResponse> mapped = coursePage.map(course ->
                CourseResponse.fromEntityWithVideos(
                        course,
                        videosByCoursePk.getOrDefault(course.getId(), Collections.emptyList())
                )
        );
        return PagedResponse.from(mapped);
    }

    @Override
    @Transactional
    public CourseVideoResponse addVideoToCourse(String courseId, AddCourseVideoRequest request) {
        Course course = courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> ApiException.notFound("Course not found"));

        CourseVideo video = CourseVideo.builder()
                .courseId(course.getId())
                .title(request.getTitle())
                .videoUrl(request.getVideoUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .durationMinutes(request.getDurationMinutes())
                .build();

        return CourseVideoResponse.fromEntity(courseVideoRepository.save(video));
    }

    @Override
    @Transactional
    public void deleteVideoFromCourse(String courseId, Long videoId) {
        Course course = courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> ApiException.notFound("Course not found"));

        CourseVideo video = courseVideoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Video not found"));

        if (!video.getCourseId().equals(course.getId())) {
            throw ApiException.notFound("Video not found for this course");
        }

        videoWatchProgressRepository.deleteByVideoId(videoId);
        courseVideoRepository.delete(video);
    }

    @Override
    @Transactional
    public void deleteCourse(String courseId) {
        Course course = courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> ApiException.notFound("Course not found"));

        courseVideoRepository.deleteByCourseId(course.getId());
        courseSubscriptionRepository.deleteByCourseId(course.getCourseId());

        List<StudyMaterial> materials =
                studyMaterialRepository.findByCourseIdOrderByCreatedAtDesc(course.getCourseId());
        for (StudyMaterial material : materials) {
            s3StorageService.deleteQuietly(material.getS3Key());
        }
        studyMaterialRepository.deleteByCourseId(course.getCourseId());

        courseRepository.delete(course);
        deleteThumbnailQuietly(course.getThumbnailUrl());
    }

    private String storeThumbnailIfPresent(MultipartFile thumbnail) {
        if (thumbnail == null || thumbnail.isEmpty()) {
            return null;
        }

        String contentType = thumbnail.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("Only image files are allowed for thumbnail (jpeg, png, gif, webp)");
        }

        String originalFileName = StringUtils.cleanPath(
                thumbnail.getOriginalFilename() != null ? thumbnail.getOriginalFilename() : "thumbnail"
        );
        String extension = extractExtension(originalFileName, contentType);
        String storedFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        Path uploadPath = Paths.get(storageDir).toAbsolutePath().normalize().resolve(THUMBNAIL_SUBDIR);
        Path target = uploadPath.resolve(storedFileName).normalize();
        if (!target.startsWith(uploadPath)) {
            throw ApiException.badRequest("Invalid file path");
        }

        try {
            Files.createDirectories(uploadPath);
            // Same write style as gallery images (Files.copy). transferTo often fails on ECS.
            Files.copy(thumbnail.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored course thumbnail at {}", target);
        } catch (IOException ex) {
            log.error("Failed to store thumbnail. path={}, user.dir={}, cause={}",
                    target, System.getProperty("user.dir"), ex.toString(), ex);
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store thumbnail file (" + uploadPath + "): " + ex.getMessage()
            );
        }

        String prefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        return prefix + "/" + THUMBNAIL_SUBDIR + "/" + storedFileName;
    }

    private String extractExtension(String originalFileName, String contentType) {
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalFileName.length() - 1) {
            return originalFileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        if (contentType == null) {
            return ".jpg";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private void deleteThumbnailQuietly(String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            return;
        }

        String prefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        String expectedPrefix = prefix + "/" + THUMBNAIL_SUBDIR + "/";
        if (!thumbnailUrl.startsWith(expectedPrefix)) {
            return;
        }

        String storedFileName = thumbnailUrl.substring(expectedPrefix.length());
        if (storedFileName.isBlank() || storedFileName.contains("..") || storedFileName.contains("/") || storedFileName.contains("\\")) {
            return;
        }

        try {
            Path uploadPath = Paths.get(storageDir).toAbsolutePath().normalize().resolve(THUMBNAIL_SUBDIR);
            Path filePath = uploadPath.resolve(storedFileName).normalize();
            if (filePath.startsWith(uploadPath)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ignored) {
            // DB is source of truth; orphaned files canj be cleaned manually if needed
        }
    }
}
