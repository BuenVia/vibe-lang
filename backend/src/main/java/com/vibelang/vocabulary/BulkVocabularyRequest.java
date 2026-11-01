package com.vibelang.vocabulary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkVocabularyRequest(@NotEmpty List<@Valid VocabularyRequest> items) {
}
