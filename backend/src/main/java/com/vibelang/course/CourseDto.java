package com.vibelang.course;

public record CourseDto(Long id, String language, String level, String description, int position) {

    public static CourseDto from(Course c) {
        return new CourseDto(c.getId(), c.getLanguage(), c.getLevel(), c.getDescription(), c.getPosition());
    }
}
