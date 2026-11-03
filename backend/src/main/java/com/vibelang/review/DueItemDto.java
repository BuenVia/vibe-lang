package com.vibelang.review;

import com.vibelang.sentence.SentenceDto;
import com.vibelang.vocabulary.VocabularyDto;

import java.time.Instant;

/** A due or new item with the vocabulary or sentence embedded, so the client needs no extra requests. */
public record DueItemDto(Type type, int box, Instant dueAt, String subjectTitle, VocabularyDto vocabulary,
                         SentenceDto sentence) {

    public enum Type { VOCABULARY, SENTENCE }
}
