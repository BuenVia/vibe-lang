package com.vibelang.review;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * One answer. Exactly one of {@code vocabularyId} / {@code sentenceId} must be set. {@code thresholdMs} is the
 * speed limit the client applied (null switches the speed rule off); {@code zone} is the learner's IANA time
 * zone, e.g. "Europe/Madrid" (UTC when missing), used to decide what "today" means.
 */
public record ReviewRequest(
        Long vocabularyId,
        Long sentenceId,
        @NotNull ReviewMode mode,
        @NotNull Boolean correct,
        @NotNull @Min(0) Integer responseMs,
        @Positive Integer thresholdMs,
        String zone) {
}
