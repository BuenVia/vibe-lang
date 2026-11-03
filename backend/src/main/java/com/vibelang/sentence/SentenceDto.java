package com.vibelang.sentence;

import java.time.Instant;

public record SentenceDto(Long id, Long subjectId, String text, String translation, String hint, String explanation,
                          int position, SentenceSource source, String wrongVersion, Instant createdAt) {

    public static SentenceDto from(Sentence s) {
        return new SentenceDto(s.getId(), s.getSubject().getId(), s.getText(), s.getTranslation(), s.getHint(),
                s.getExplanation(), s.getPosition(), s.getSource(), s.getWrongVersion(), s.getCreatedAt());
    }
}
