package com.vibelang.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 200) String title,
        String description) {
}
