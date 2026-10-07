package com.academy.project.dto.material;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponse {

    private Long materialId;
    private String fileName;
    private String contentType;
    private String downloadUrl;
    private long expiresInSeconds;
}
