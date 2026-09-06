package com.academy.project.dto.image;

import com.academy.project.entity.image.GalleryImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ImageResponse {

    private Long id;
    private String originalFileName;
    private String imageUrl;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ImageResponse fromEntity(GalleryImage image) {
        return ImageResponse.builder()
                .id(image.getId())
                .originalFileName(image.getOriginalFileName())
                .imageUrl(image.getImageUrl())
                .contentType(image.getContentType())
                .fileSize(image.getFileSize())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
}
