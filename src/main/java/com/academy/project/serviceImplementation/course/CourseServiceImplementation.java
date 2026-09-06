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
import com.academy.project.repository.course.CourseRepository;
import com.academy.project.repository.course.CourseVideoRepository;
import com.academy.project.service.course.CourseService;
import com.academy.project.util.CourseIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImplementation implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseVideoRepository courseVideoRepository;

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .durationHours(request.getDurationHours())
                .price(request.getPrice())
                .status(request.getStatus() != null ? request.getStatus() : CourseStatus.ACTIVE)
                .thumbnailUrl(request.getThumbnailUrl())
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

        Page<CourseResponse> mapped = coursePage.map(CourseResponse::fromEntity);
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
}
