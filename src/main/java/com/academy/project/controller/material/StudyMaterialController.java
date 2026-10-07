package com.academy.project.controller.material;

import com.academy.project.dto.material.PresignedUrlResponse;
import com.academy.project.dto.material.StudyMaterialResponse;
import com.academy.project.dto.response.ApiResponse;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.service.material.StudyMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study-materials")
@RequiredArgsConstructor
@CrossOrigin
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;

    /**
     * Lists materials for the caller's active subscriptions.
     * Pass {@code courseId} to filter to one subscribed course.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StudyMaterialResponse>>> list(
            @RequestParam(required = false) String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<StudyMaterialResponse> response =
                studyMaterialService.listForUser(courseId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Study materials fetched successfully", response));
    }

    /** Metadata + fresh pre-signed download URL (requires active course subscription). */
    @GetMapping("/{materialId}")
    public ResponseEntity<ApiResponse<StudyMaterialResponse>> get(@PathVariable Long materialId) {
        StudyMaterialResponse response = studyMaterialService.getForUser(materialId);
        return ResponseEntity.ok(ApiResponse.ok("Study material fetched successfully", response));
    }

    /** Fresh short-lived pre-signed GET URL for the S3 object. */
    @GetMapping("/{materialId}/download-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> downloadUrl(
            @PathVariable Long materialId) {
        PresignedUrlResponse response = studyMaterialService.getDownloadUrl(materialId);
        return ResponseEntity.ok(ApiResponse.ok("Download URL generated successfully", response));
    }
}
