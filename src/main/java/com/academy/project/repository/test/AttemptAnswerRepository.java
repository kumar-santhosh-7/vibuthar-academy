package com.academy.project.repository.test;

import com.academy.project.entity.test.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByAttemptId(Long attemptId);

    void deleteByAttemptIdIn(Collection<Long> attemptIds);
}
