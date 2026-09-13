package com.academy.project.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class ImageStorageInitializer {

    private static final String THUMBNAIL_SUBDIR = "thumbnails";

    @Value("${app.images.storage-dir:images}")
    private String storageDir;

    @PostConstruct
    public void ensureStorageDirectories() {
        Path imagesPath = Paths.get(storageDir).toAbsolutePath().normalize();
        Path thumbnailsPath = imagesPath.resolve(THUMBNAIL_SUBDIR).normalize();

        try {
            Files.createDirectories(thumbnailsPath);
            if (!Files.isWritable(imagesPath) || !Files.isWritable(thumbnailsPath)) {
                log.warn("Image storage directories exist but are not writable. images={}, thumbnails={}, user.dir={}",
                        imagesPath, thumbnailsPath, System.getProperty("user.dir"));
                return;
            }
            log.info("Image storage ready. images={}, thumbnails={}, user.dir={}",
                    imagesPath, thumbnailsPath, System.getProperty("user.dir"));
        } catch (Exception ex) {
            // Do not block startup; uploads will fail with a clear error until storage is fixed.
            log.warn("Could not prepare image storage at {} (user.dir={}): {}",
                    thumbnailsPath, System.getProperty("user.dir"), ex.toString());
        }
    }
}
