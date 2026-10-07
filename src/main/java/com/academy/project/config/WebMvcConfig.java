package com.academy.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.images.storage-dir:images}")
    private String storageDir;

    @Value("${app.images.url-prefix:/images}")
    private String urlPrefix;

    @Value("${app.pdfs.storage-dir:pdfs}")
    private String pdfStorageDir;

    @Value("${app.pdfs.url-prefix:/pdfs}")
    private String pdfUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        addHandler(registry, storageDir, urlPrefix);
        addHandler(registry, pdfStorageDir, pdfUrlPrefix);
    }

    private void addHandler(ResourceHandlerRegistry registry, String dir, String prefix) {
        String location = Paths.get(dir).toAbsolutePath().normalize().toUri().toString();
        String pattern = prefix.endsWith("/") ? prefix + "**" : prefix + "/**";
        registry.addResourceHandler(pattern)
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
