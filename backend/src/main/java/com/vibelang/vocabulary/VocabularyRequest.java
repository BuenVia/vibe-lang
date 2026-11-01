package com.vibelang.vocabulary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VocabularyRequest(
        @NotBlank @Size(max = 500) String term,
        @NotBlank @Size(max = 500) String translation,
        String notes) {
}
