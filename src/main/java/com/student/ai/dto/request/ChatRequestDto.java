package com.student.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ChatRequestDto(@NotEmpty @NotBlank String message) {
}
