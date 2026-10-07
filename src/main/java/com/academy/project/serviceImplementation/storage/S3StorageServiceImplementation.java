package com.academy.project.serviceImplementation.storage;

import com.academy.project.exception.ApiException;
import com.academy.project.service.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImplementation implements S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.s3.presign-duration-minutes:15}")
    private long presignDurationMinutes;

    @Override
    public String upload(String objectKey, MultipartFile file) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            log.info("Uploaded study material to s3://{}/{}", bucket, objectKey);
            return objectKey;
        } catch (IOException | S3Exception ex) {
            log.error("Failed to upload to S3. bucket={}, key={}, cause={}",
                    bucket, objectKey, ex.toString());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload file to S3: " + ex.getMessage());
        }
    }

    @Override
    public void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            log.info("Deleted S3 object s3://{}/{}", bucket, objectKey);
        } catch (Exception ex) {
            log.warn("Could not delete S3 object {}: {}", objectKey, ex.toString());
        }
    }

    @Override
    public String createPresignedGetUrl(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignDurationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toExternalForm();
        } catch (S3Exception ex) {
            log.error("Failed to create pre-signed URL. bucket={}, key={}, cause={}",
                    bucket, objectKey, ex.toString());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create download URL");
        }
    }

    @Override
    public long getPresignDurationSeconds() {
        return Duration.ofMinutes(presignDurationMinutes).toSeconds();
    }
}
