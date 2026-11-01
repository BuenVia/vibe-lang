package com.vibelang.subject;

public record SubjectDto(
        Long id,
        Long categoryId,
        String categoryName,
        Long courseId,
        String title,
        String description,
        int position,
        boolean hasLesson,
        long vocabularyCount) {

    public static SubjectDto from(Subject s, boolean hasLesson, long vocabularyCount) {
        return new SubjectDto(
                s.getId(),
                s.getCategory().getId(),
                s.getCategory().getName(),
                s.getCategory().getCourse().getId(),
                s.getTitle(),
                s.getDescription(),
                s.getPosition(),
                hasLesson,
                vocabularyCount);
    }
}
