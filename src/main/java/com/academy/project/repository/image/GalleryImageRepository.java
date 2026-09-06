package com.academy.project.repository.image;

import com.academy.project.entity.image.GalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {

    long count();
}
