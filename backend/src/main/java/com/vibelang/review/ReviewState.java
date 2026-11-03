package com.vibelang.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Spaced-repetition state of one vocabulary item or sentence (exactly one of the two ids is set). */
@Entity
@Table(name = "review_state")
public class ReviewState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vocabulary_id")
    private Long vocabularyId;

    @Column(name = "sentence_id")
    private Long sentenceId;

    @Column(nullable = false)
    private int box;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "first_reviewed_at", nullable = false)
    private Instant firstReviewedAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "last_response_ms")
    private Integer lastResponseMs;

    @Column(nullable = false)
    private int reviews;

    @Column(nullable = false)
    private int lapses;

    public Long getId() { return id; }
    public Long getVocabularyId() { return vocabularyId; }
    public void setVocabularyId(Long vocabularyId) { this.vocabularyId = vocabularyId; }
    public Long getSentenceId() { return sentenceId; }
    public void setSentenceId(Long sentenceId) { this.sentenceId = sentenceId; }
    public int getBox() { return box; }
    public void setBox(int box) { this.box = box; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getFirstReviewedAt() { return firstReviewedAt; }
    public void setFirstReviewedAt(Instant firstReviewedAt) { this.firstReviewedAt = firstReviewedAt; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
    public Integer getLastResponseMs() { return lastResponseMs; }
    public void setLastResponseMs(Integer lastResponseMs) { this.lastResponseMs = lastResponseMs; }
    public int getReviews() { return reviews; }
    public void setReviews(int reviews) { this.reviews = reviews; }
    public int getLapses() { return lapses; }
    public void setLapses(int lapses) { this.lapses = lapses; }
}
