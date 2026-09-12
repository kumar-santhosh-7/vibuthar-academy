package com.academy.project.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
    public void ensureStorageDirectories() throws IOException {
        Path imagesPath = Paths.get(storageDir).toAbsolutePath().normalize();
        Path thumbnailsPath = imagesPath.resolve(THUMBNAIL_SUBDIR).normalize();

        Files.createDirectories(thumbnailsPath);

        if (!Files.isWritable(imagesPath) || !Files.isWritable(thumbnailsPath)) {
            throw new IllegalStateException(
                    "Image storage directories are not writable. images=" + imagesPath
                            + ", thumbnails=" + thumbnailsPath
                            + ", user.dir=" + System.getProperty("user.dir")
            );
        }

        log.info("Image storage ready. images={}, thumbnails={}, user.dir={}",
                imagesPath, thumbnailsPath, System.getProperty("user.dir"));
    }
}
