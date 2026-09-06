package com.academy.project.repository.course;

import com.academy.project.entity.course.CourseVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseVideoRepository extends JpaRepository<CourseVideo, Long> {

    List<CourseVideo> findByCourseIdOrderBySortOrderAsc(Long courseId);

    List<CourseVideo> findByCourseIdInOrderBySortOrderAsc(Collection<Long> courseIds);
}
