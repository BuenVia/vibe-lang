package com.vibelang.lesson;

import java.time.Instant;

public record LessonDto(Long id, Long subjectId, String content, Instant updatedAt) {

    public static LessonDto from(Lesson l) {
        return new LessonDto(l.getId(), l.getSubject().getId(), l.getContent(), l.getUpdatedAt());
    }
}
