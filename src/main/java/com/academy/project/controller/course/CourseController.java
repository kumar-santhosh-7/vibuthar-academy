package com.academy.project.controller.course;

import com.academy.project.dto.course.AddCourseVideoRequest;
import com.academy.project.dto.course.CreateCourseRequest;
import com.academy.project.dto.course.CourseResponse;
import com.academy.project.dto.course.CourseVideoResponse;
import com.academy.project.dto.response.ApiResponse;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.enums.CourseStatus;
import com.academy.project.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin
public class CourseController {

    private final CourseService courseService;

    @PostMapping("/admin/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CreateCourseRequest request) {
        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Course created successfully", response));
    }

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<PagedResponse<CourseResponse>>> listPublicCourses(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CourseResponse> response = courseService.listCourses(
                CourseStatus.ACTIVE, search, page, size
        );
        return ResponseEntity.ok(ApiResponse.ok("Courses fetched successfully", response));
    }

    @GetMapping("/admin/courses")
    public ResponseEntity<ApiResponse<PagedResponse<CourseResponse>>> listAdminCourses(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CourseResponse> response = courseService.listCourses(status, search, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Courses fetched successfully", response));
    }

    @PostMapping("/admin/courses/{courseId}/videos")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<ApiResponse<CourseVideoResponse>> addVideoToCourse(
            @PathVariable String courseId,
            @Valid @RequestBody AddCourseVideoRequest request) {
        CourseVideoResponse response = courseService.addVideoToCourse(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Video added successfully", response));
    }
}
