package com.vibelang.category;

public record CategoryDto(Long id, Long courseId, String name, String description, int position) {

    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getCourse().getId(), c.getName(), c.getDescription(), c.getPosition());
    }
}
