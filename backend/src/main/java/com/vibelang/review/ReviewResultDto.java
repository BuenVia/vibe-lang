package com.vibelang.review;

import java.time.Instant;

public record ReviewResultDto(Long vocabularyId, Long sentenceId, int box, Instant dueAt,
                              ReviewScheduler.Outcome outcome, int reviews, int lapses) {

    static ReviewResultDto from(ReviewState s, ReviewScheduler.Outcome outcome) {
        return new ReviewResultDto(s.getVocabularyId(), s.getSentenceId(), s.getBox(), s.getDueAt(), outcome,
                s.getReviews(), s.getLapses());
    }
}
