package com.academy.project.repository.test;

import com.academy.project.entity.test.OnlineTest;
import com.academy.project.enums.TestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnlineTestRepository extends JpaRepository<OnlineTest, Long> {

    Optional<OnlineTest> findByTestId(String testId);

    Page<OnlineTest> findByStatus(TestStatus status, Pageable pageable);

    Page<OnlineTest> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<OnlineTest> findByStatusAndTitleContainingIgnoreCase(
            TestStatus status, String title, Pageable pageable);
}
