package com.academy.project.repository.material;

import com.academy.project.entity.material.StudyMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    Page<StudyMaterial> findByCourseId(String courseId, Pageable pageable);

    Page<StudyMaterial> findByCourseIdIn(Collection<String> courseIds, Pageable pageable);

    Page<StudyMaterial> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<StudyMaterial> findByCourseIdAndTitleContainingIgnoreCase(
            String courseId, String title, Pageable pageable);

    List<StudyMaterial> findByCourseIdOrderByCreatedAtDesc(String courseId);

    void deleteByCourseId(String courseId);
}
