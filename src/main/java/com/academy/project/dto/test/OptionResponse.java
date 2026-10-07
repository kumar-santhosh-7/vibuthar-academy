package com.academy.project.dto.test;

import com.academy.project.entity.test.QuestionOption;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptionResponse {

    private Long id;
    private String optionText;
    private Integer orderIndex;
    /** Present only for admin responses. */
    private Boolean correct;

    public static OptionResponse from(QuestionOption option, boolean includeCorrect) {
        return OptionResponse.builder()
                .id(option.getId())
                .optionText(option.getOptionText())
                .orderIndex(option.getOrderIndex())
                .correct(includeCorrect ? option.isCorrect() : null)
                .build();
    }
}
