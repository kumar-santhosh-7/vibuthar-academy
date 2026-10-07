package com.academy.project.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface S3StorageService {

    /**
     * Uploads a file to S3 under the given object key.
     *
     * @return the same object key on success
     */
    String upload(String objectKey, MultipartFile file);

    /** Deletes an object if it exists; does not throw when missing. */
    void deleteQuietly(String objectKey);

    /** Creates a short-lived pre-signed GET URL for the object. */
    String createPresignedGetUrl(String objectKey);

    long getPresignDurationSeconds();
}
