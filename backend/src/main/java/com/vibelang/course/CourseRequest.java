package com.vibelang.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank @Size(max = 100) String language,
        @NotBlank @Size(max = 20) String level,
        String description) {
}
