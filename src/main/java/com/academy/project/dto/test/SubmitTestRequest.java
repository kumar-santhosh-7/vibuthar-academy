package com.academy.project.dto.test;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubmitTestRequest {

    @NotEmpty(message = "Answers are required")
    @Valid
    private List<AnswerItem> answers;

    @Getter
    @Setter
    public static class AnswerItem {

        @NotNull(message = "questionId is required")
        private Long questionId;

        @NotNull(message = "optionId is required")
        private Long optionId;
    }
}
