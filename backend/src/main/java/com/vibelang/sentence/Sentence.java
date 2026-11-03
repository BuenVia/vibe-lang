package com.vibelang.sentence;

import com.vibelang.subject.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** A sentence in the target language with one or more gaps, written as {@code {{answer}}}, for cloze practice. */
@Entity
@Table(name = "sentence")
public class Sentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /** The sentence with gaps, e.g. "No creo que {{tenga}} razón." */
    @Column(nullable = false, length = 1000)
    private String text;

    /** Optional translation of the whole sentence, shown as a prompt. */
    @Column(length = 1000)
    private String translation;

    /** Optional short hint shown next to the gap, e.g. the infinitive "tener". */
    @Column(length = 200)
    private String hint;

    /** Optional explanation of the rule, shown after answering. */
    @Column(columnDefinition = "text")
    private String explanation;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SentenceSource source = SentenceSource.MANUAL;

    /** For a mistake: what the learner actually said or wrote. */
    @Column(name = "wrong_version", length = 1000)
    private String wrongVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public SentenceSource getSource() { return source; }
    public void setSource(SentenceSource source) { this.source = source; }
    public String getWrongVersion() { return wrongVersion; }
    public void setWrongVersion(String wrongVersion) { this.wrongVersion = wrongVersion; }
    public Instant getCreatedAt() { return createdAt; }
}
