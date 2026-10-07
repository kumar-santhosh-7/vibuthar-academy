package com.academy.project.dto.test;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AddQuestionsRequest {

    @NotEmpty(message = "At least one question is required")
    @Valid
    private List<QuestionItem> questions;

    @Getter
    @Setter
    public static class QuestionItem {

        @NotBlank(message = "Question text is required")
        private String questionText;

        private Integer orderIndex;

        @NotEmpty(message = "At least two options are required")
        @Valid
        private List<OptionItem> options;
    }

    @Getter
    @Setter
    public static class OptionItem {

        @NotBlank(message = "Option text is required")
        private String optionText;

        @NotNull(message = "correct flag is required")
        private Boolean correct;

        private Integer orderIndex;
    }
}
