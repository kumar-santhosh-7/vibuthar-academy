package com.academy.project.dto.test;

import com.academy.project.entity.test.QuestionOption;
import com.academy.project.entity.test.TestQuestion;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionResponse {

    private Long id;
    private String questionText;
    private Integer orderIndex;
    private List<OptionResponse> options;

    public static QuestionResponse from(TestQuestion question, List<QuestionOption> options, boolean includeCorrect) {
        return QuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .orderIndex(question.getOrderIndex())
                .options(options.stream()
                        .map(opt -> OptionResponse.from(opt, includeCorrect))
                        .toList())
                .build();
    }
}
