package com.academy.project.repository.video;

import com.academy.project.entity.video.VideoWatchProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VideoWatchProgressRepository extends JpaRepository<VideoWatchProgress, Long> {

    Optional<VideoWatchProgress> findByUserIdAndVideoId(String userId, Long videoId);

    List<VideoWatchProgress> findByUserIdAndCourseId(String userId, String courseId);

    List<VideoWatchProgress> findByUserIdAndCourseIdIn(String userId, Collection<String> courseIds);

    long countByUserIdAndCourseIdAndCompletedTrue(String userId, String courseId);

    long countByVideoIdAndCompletedTrue(Long videoId);

    void deleteByUserId(String userId);

    void deleteByVideoId(Long videoId);

    @Query("""
            SELECT p.videoId, COUNT(p)
            FROM VideoWatchProgress p
            WHERE p.videoId IN :videoIds AND p.completed = true
            GROUP BY p.videoId
            """)
    List<Object[]> countCompletedByVideoIds(@Param("videoIds") Collection<Long> videoIds);

    @Query("""
            SELECT p.userId, COUNT(p)
            FROM VideoWatchProgress p
            WHERE p.userId IN :userIds
              AND p.completed = true
              AND (:courseId IS NULL OR p.courseId = :courseId)
            GROUP BY p.userId
            """)
    List<Object[]> countCompletedByUserIds(
            @Param("userIds") Collection<String> userIds,
            @Param("courseId") String courseId
    );
}
