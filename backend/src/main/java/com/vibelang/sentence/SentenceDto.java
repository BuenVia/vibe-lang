package com.vibelang.sentence;

public record SentenceDto(Long id, Long subjectId, String text, String translation, String hint, String explanation,
                          int position) {

    public static SentenceDto from(Sentence s) {
        return new SentenceDto(s.getId(), s.getSubject().getId(), s.getText(), s.getTranslation(), s.getHint(),
                s.getExplanation(), s.getPosition());
    }
}
