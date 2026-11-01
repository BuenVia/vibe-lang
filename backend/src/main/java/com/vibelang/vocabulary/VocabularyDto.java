package com.vibelang.vocabulary;

public record VocabularyDto(Long id, Long subjectId, String term, String translation, String notes, int position) {

    public static VocabularyDto from(Vocabulary v) {
        return new VocabularyDto(v.getId(), v.getSubject().getId(), v.getTerm(), v.getTranslation(), v.getNotes(),
                v.getPosition());
    }
}
