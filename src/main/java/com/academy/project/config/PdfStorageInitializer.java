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
public class PdfStorageInitializer {

    @Value("${app.pdfs.storage-dir:pdfs}")
    private String storageDir;

    @PostConstruct
    public void ensureStorageDirectory() {
        Path pdfsPath = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(pdfsPath);
            if (!Files.isWritable(pdfsPath)) {
                log.warn("PDF storage directory exists but is not writable. path={}, user.dir={}",
                        pdfsPath, System.getProperty("user.dir"));
                return;
            }
            log.info("PDF storage ready. path={}, user.dir={}",
                    pdfsPath, System.getProperty("user.dir"));
        } catch (Exception ex) {
            log.warn("Could not prepare PDF storage at {} (user.dir={}): {}",
                    pdfsPath, System.getProperty("user.dir"), ex.toString());
        }
    }
}
