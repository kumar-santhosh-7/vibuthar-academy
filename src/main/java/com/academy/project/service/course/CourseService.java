package com.academy.project.service.course;

import com.academy.project.dto.course.AddCourseVideoRequest;
import com.academy.project.dto.course.CreateCourseRequest;
import com.academy.project.dto.course.CourseResponse;
import com.academy.project.dto.course.CourseVideoResponse;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.enums.CourseStatus;

public interface CourseService {

    CourseResponse createCourse(CreateCourseRequest request);

    PagedResponse<CourseResponse> listCourses(CourseStatus status, String search, int page, int size);

    CourseVideoResponse addVideoToCourse(String courseId, AddCourseVideoRequest request);
}
