package com.academy.project.controller.admin;

import com.academy.project.dto.material.StudyMaterialResponse;
import com.academy.project.dto.material.UpdateStudyMaterialRequest;
import com.academy.project.dto.response.ApiResponse;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.service.material.StudyMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/study-materials")
@RequiredArgsConstructor
@CrossOrigin
public class AdminStudyMaterialController {

    private final StudyMaterialService studyMaterialService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterialResponse>> upload(
            @RequestParam("courseId") String courseId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file) {
        StudyMaterialResponse response =
                studyMaterialService.upload(courseId, title, description, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Study material uploaded successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<StudyMaterialResponse>>> list(
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<StudyMaterialResponse> response =
                studyMaterialService.listForAdmin(courseId, search, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Study materials fetched successfully", response));
    }

    @GetMapping("/{materialId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterialResponse>> get(@PathVariable Long materialId) {
        StudyMaterialResponse response = studyMaterialService.getForAdmin(materialId);
        return ResponseEntity.ok(ApiResponse.ok("Study material fetched successfully", response));
    }

    @PutMapping("/{materialId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterialResponse>> update(
            @PathVariable Long materialId,
            @Valid @RequestBody UpdateStudyMaterialRequest request) {
        StudyMaterialResponse response = studyMaterialService.update(materialId, request);
        return ResponseEntity.ok(ApiResponse.ok("Study material updated successfully", response));
    }

    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long materialId) {
        studyMaterialService.delete(materialId);
        return ResponseEntity.ok(ApiResponse.ok("Study material deleted successfully", null));
    }
}
