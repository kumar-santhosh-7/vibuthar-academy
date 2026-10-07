package com.academy.project.service.material;

import com.academy.project.dto.material.PresignedUrlResponse;
import com.academy.project.dto.material.StudyMaterialResponse;
import com.academy.project.dto.material.UpdateStudyMaterialRequest;
import com.academy.project.dto.response.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StudyMaterialService {

    StudyMaterialResponse upload(String courseId, String title, String description, MultipartFile file);

    StudyMaterialResponse update(Long materialId, UpdateStudyMaterialRequest request);

    void delete(Long materialId);

    StudyMaterialResponse getForAdmin(Long materialId);

    PagedResponse<StudyMaterialResponse> listForAdmin(String courseId, String search, int page, int size);

    StudyMaterialResponse getForUser(Long materialId);

    PagedResponse<StudyMaterialResponse> listForUser(String courseId, int page, int size);

    PresignedUrlResponse getDownloadUrl(Long materialId);
}
