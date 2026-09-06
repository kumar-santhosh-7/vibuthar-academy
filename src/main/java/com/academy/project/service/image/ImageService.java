package com.academy.project.service.image;

import com.academy.project.dto.image.ImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {

    List<ImageResponse> listAll();

    ImageResponse getById(Long imageId);

    ImageResponse upload(MultipartFile file);

    ImageResponse update(Long imageId, MultipartFile file);

    void delete(Long imageId);
}
