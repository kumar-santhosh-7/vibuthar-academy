package com.academy.project.repository.test;

import com.academy.project.entity.test.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionIdOrderByOrderIndexAscIdAsc(Long questionId);

    List<QuestionOption> findByQuestionIdInOrderByOrderIndexAscIdAsc(Collection<Long> questionIds);

    void deleteByQuestionId(Long questionId);

    void deleteByQuestionIdIn(Collection<Long> questionIds);
}
