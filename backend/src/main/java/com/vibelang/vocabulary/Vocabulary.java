package com.vibelang.vocabulary;

import com.vibelang.subject.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vocabulary")
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 500)
    private String term;

    @Column(nullable = false, length = 500)
    private String translation;

    /** Optional example sentence or usage note, shown on the back of the flashcard. */
    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private int position;

    public Long getId() { return id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
