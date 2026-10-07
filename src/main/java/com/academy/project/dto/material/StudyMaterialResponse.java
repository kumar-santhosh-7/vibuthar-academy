package com.academy.project.dto.material;

import com.academy.project.entity.material.StudyMaterial;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudyMaterialResponse {

    private Long id;
    private String courseId;
    private String title;
    private String description;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String uploadedBy;
    /** Short-lived S3 pre-signed GET URL (only when requested). */
    private String downloadUrl;
    private Long downloadUrlExpiresInSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StudyMaterialResponse fromEntity(StudyMaterial material) {
        return StudyMaterialResponse.builder()
                .id(material.getId())
                .courseId(material.getCourseId())
                .title(material.getTitle())
                .description(material.getDescription())
                .originalFileName(material.getOriginalFileName())
                .contentType(material.getContentType())
                .fileSize(material.getFileSize())
                .uploadedBy(material.getUploadedBy())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }

    public static StudyMaterialResponse withDownloadUrl(
            StudyMaterial material,
            String downloadUrl,
            long expiresInSeconds) {
        return StudyMaterialResponse.builder()
                .id(material.getId())
                .courseId(material.getCourseId())
                .title(material.getTitle())
                .description(material.getDescription())
                .originalFileName(material.getOriginalFileName())
                .contentType(material.getContentType())
                .fileSize(material.getFileSize())
                .uploadedBy(material.getUploadedBy())
                .downloadUrl(downloadUrl)
                .downloadUrlExpiresInSeconds(expiresInSeconds)
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }
}
