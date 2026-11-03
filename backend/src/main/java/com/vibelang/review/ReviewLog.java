package com.vibelang.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One reported answer. Write-only for now; kept for statistics. */
@Entity
@Table(name = "review_log")
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vocabulary_id")
    private Long vocabularyId;

    @Column(name = "sentence_id")
    private Long sentenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewMode mode;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "response_ms", nullable = false)
    private int responseMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewScheduler.Outcome outcome;

    @Column(name = "box_after", nullable = false)
    private int boxAfter;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected ReviewLog() {}

    public ReviewLog(Long vocabularyId, Long sentenceId, ReviewMode mode, boolean correct, int responseMs,
                     ReviewScheduler.Outcome outcome, int boxAfter, Instant reviewedAt) {
        this.vocabularyId = vocabularyId;
        this.sentenceId = sentenceId;
        this.mode = mode;
        this.correct = correct;
        this.responseMs = responseMs;
        this.outcome = outcome;
        this.boxAfter = boxAfter;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() { return id; }
}
