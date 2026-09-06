package com.academy.project.controller.admin;

import com.academy.project.dto.image.ImageResponse;
import com.academy.project.dto.response.ApiResponse;
import com.academy.project.service.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class ImageController {

    private final ImageService imageService;

    /** Public fetch — no login required. */
    @GetMapping("/api/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> listImages() {
        List<ImageResponse> response = imageService.listAll();
        return ResponseEntity.ok(ApiResponse.ok("Images fetched successfully", response));
    }

    /** Public fetch by id — no login required. */
    @GetMapping("/api/images/{imageId}")
    public ResponseEntity<ApiResponse<ImageResponse>> getImage(@PathVariable Long imageId) {
        ImageResponse response = imageService.getById(imageId);
        return ResponseEntity.ok(ApiResponse.ok("Image fetched successfully", response));
    }

    @PostMapping(value = "/api/admin/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImageResponse>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        ImageResponse response = imageService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Image uploaded successfully", response));
    }

    @PutMapping(value = "/api/admin/images/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImageResponse>> updateImage(
            @PathVariable Long imageId,
            @RequestParam("file") MultipartFile file) {
        ImageResponse response = imageService.update(imageId, file);
        return ResponseEntity.ok(ApiResponse.ok("Image updated successfully", response));
    }

    @DeleteMapping("/api/admin/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long imageId) {
        imageService.delete(imageId);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted successfully", null));
    }
}
