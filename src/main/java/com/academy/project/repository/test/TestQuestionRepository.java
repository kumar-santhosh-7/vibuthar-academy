package com.academy.project.repository.test;

import com.academy.project.entity.test.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {

    List<TestQuestion> findByTestPkOrderByOrderIndexAscIdAsc(Long testPk);

    long countByTestPk(Long testPk);

    void deleteByTestPk(Long testPk);
}
