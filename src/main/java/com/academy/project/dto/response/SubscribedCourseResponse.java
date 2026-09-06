package com.academy.project.dto.response;

import com.academy.project.dto.course.CourseVideoResponse;
import com.academy.project.entity.course.Course;
import com.academy.project.entity.course.CourseVideo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class SubscribedCourseResponse {

    private String courseId;
    private String title;
    private LocalDateTime subscribedAt;
    @Builder.Default
    private List<CourseVideoResponse> videos = Collections.emptyList();

    public static SubscribedCourseResponse from(Course course, LocalDateTime subscribedAt) {
        return from(course, subscribedAt, Collections.emptyList());
    }

    public static SubscribedCourseResponse from(
            Course course, LocalDateTime subscribedAt, List<CourseVideo> videos) {
        List<CourseVideoResponse> videoResponses = videos == null
                ? Collections.emptyList()
                : videos.stream().map(CourseVideoResponse::fromEntity).toList();
        return SubscribedCourseResponse.builder()
                .courseId(course.getCourseId())
                .title(course.getTitle())
                .subscribedAt(subscribedAt)
                .videos(videoResponses)
                .build();
    }
}
