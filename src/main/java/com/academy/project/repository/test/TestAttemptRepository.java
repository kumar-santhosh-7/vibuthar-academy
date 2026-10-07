package com.academy.project.repository.test;

import com.academy.project.entity.test.TestAttempt;
import com.academy.project.enums.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    Optional<TestAttempt> findByTestPkAndUserId(Long testPk, String userId);

    boolean existsByTestPkAndUserId(Long testPk, String userId);

    boolean existsByTestPkAndUserIdAndStatus(Long testPk, String userId, AttemptStatus status);

    List<TestAttempt> findByTestPkAndStatusOrderBySubmittedAtDesc(Long testPk, AttemptStatus status);

    List<TestAttempt> findByTestPkOrderBySubmittedAtDesc(Long testPk);

    void deleteByTestPk(Long testPk);

    List<TestAttempt> findByUserId(String userId);

    void deleteByUserId(String userId);
}
