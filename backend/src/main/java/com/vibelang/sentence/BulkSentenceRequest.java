package com.vibelang.sentence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkSentenceRequest(@NotEmpty List<@Valid SentenceRequest> items) {
}
