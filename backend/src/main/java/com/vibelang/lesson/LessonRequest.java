package com.vibelang.lesson;

import jakarta.validation.constraints.NotNull;

public record LessonRequest(@NotNull String content) {
}
