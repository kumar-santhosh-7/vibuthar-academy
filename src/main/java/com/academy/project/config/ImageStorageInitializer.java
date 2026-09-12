package com.academy.project.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ensures {@code app.images.storage-dir} exists at startup.
 * Failures are logged, not thrown, so a missing or non-writable directory
 * does not prevent the rest of the API from starting (uploads will still fail
 * until the path is writable).
 */
@Slf4j
@Component
public class ImageStorageInitializer implements ApplicationRunner {

    @Value("${app.images.storage-dir:images}")
    private String storageDir;

    @Override
    public void run(ApplicationArguments args) {
        Path path = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
            log.info("Image storage directory ready: {}", path);
        } catch (Exception ex) {
            log.warn(
                    "Could not create image storage directory {}. Gallery uploads will fail until it is writable: {}",
                    path,
                    ex.getMessage()
            );
        }
    }
}
