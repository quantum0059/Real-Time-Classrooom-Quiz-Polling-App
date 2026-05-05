package com.quizapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerRequest {
    @NotNull(message = "Student database id is required")
    private Long studentId;

    @NotNull(message = "Question id is required")
    private Long questionId;

    @NotNull(message = "Selected option index is required")
    @Min(value = 0, message = "Selected option index must be zero or greater")
    private Integer selectedOption;

    @NotBlank(message = "Session code is required")
    private String sessionCode;
}
