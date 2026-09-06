package com.academy.project.serviceImplementation.image;

import com.academy.project.dto.image.ImageResponse;
import com.academy.project.entity.image.GalleryImage;
import com.academy.project.exception.ApiException;
import com.academy.project.repository.image.GalleryImageRepository;
import com.academy.project.service.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImplementation implements ImageService {

    private static final int MAX_IMAGES = 10;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final GalleryImageRepository galleryImageRepository;

    @Value("${app.images.storage-dir:images}")
    private String storageDir;

    @Value("${app.images.url-prefix:/images}")
    private String urlPrefix;

    @Override
    @Transactional(readOnly = true)
    public List<ImageResponse> listAll() {
        return galleryImageRepository.findAll().stream()
                .sorted(Comparator.comparing(GalleryImage::getCreatedAt).reversed())
                .map(ImageResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ImageResponse getById(Long imageId) {
        GalleryImage image = galleryImageRepository.findById(imageId)
                .orElseThrow(() -> ApiException.notFound("Image not found"));
        return ImageResponse.fromEntity(image);
    }

    @Override
    @Transactional
    public ImageResponse upload(MultipartFile file) {
        validateSingleImage(file);

        if (galleryImageRepository.count() >= MAX_IMAGES) {
            throw ApiException.badRequest(
                    "Maximum of 10 images allowed. Please remove existing images and add new ones."
            );
        }

        StoredFile stored = storeFile(file);

        GalleryImage image = GalleryImage.builder()
                .originalFileName(stored.originalFileName())
                .storedFileName(stored.storedFileName())
                .contentType(stored.contentType())
                .fileSize(stored.fileSize())
                .imageUrl(buildImageUrl(stored.storedFileName()))
                .build();

        return ImageResponse.fromEntity(galleryImageRepository.save(image));
    }

    @Override
    @Transactional
    public ImageResponse update(Long imageId, MultipartFile file) {
        validateSingleImage(file);

        GalleryImage existing = galleryImageRepository.findById(imageId)
                .orElseThrow(() -> ApiException.notFound("Image not found"));

        String previousStoredName = existing.getStoredFileName();
        StoredFile stored = storeFile(file);

        existing.setOriginalFileName(stored.originalFileName());
        existing.setStoredFileName(stored.storedFileName());
        existing.setContentType(stored.contentType());
        existing.setFileSize(stored.fileSize());
        existing.setImageUrl(buildImageUrl(stored.storedFileName()));

        GalleryImage saved = galleryImageRepository.save(existing);
        deleteFileQuietly(previousStoredName);

        return ImageResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void delete(Long imageId) {
        GalleryImage existing = galleryImageRepository.findById(imageId)
                .orElseThrow(() -> ApiException.notFound("Image not found"));

        galleryImageRepository.delete(existing);
        deleteFileQuietly(existing.getStoredFileName());
    }

    private void validateSingleImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Please upload exactly one image file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("Only image files are allowed (jpeg, png, gif, webp)");
        }
    }

    private StoredFile storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "image"
        );
        String extension = extractExtension(originalFileName, file.getContentType());
        String storedFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path uploadPath = resolveStoragePath();
            Files.createDirectories(uploadPath);
            Path target = uploadPath.resolve(storedFileName).normalize();
            if (!target.startsWith(uploadPath)) {
                throw ApiException.badRequest("Invalid file path");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store image file");
        }

        return new StoredFile(
                originalFileName,
                storedFileName,
                file.getContentType(),
                file.getSize()
        );
    }

    private Path resolveStoragePath() {
        return Paths.get(storageDir).toAbsolutePath().normalize();
    }

    private String buildImageUrl(String storedFileName) {
        String prefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        return prefix + "/" + storedFileName;
    }

    private String extractExtension(String originalFileName, String contentType) {
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalFileName.length() - 1) {
            return originalFileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        if (contentType == null) {
            return ".jpg";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private void deleteFileQuietly(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        try {
            Path filePath = resolveStoragePath().resolve(storedFileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // DB is source of truth; orphaned files can be cleaned manually if needed
        }
    }

    private record StoredFile(
            String originalFileName,
            String storedFileName,
            String contentType,
            long fileSize
    ) {
    }
}
