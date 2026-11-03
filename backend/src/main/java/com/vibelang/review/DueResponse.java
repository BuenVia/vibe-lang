package com.vibelang.review;

import java.util.List;

/** {@code dueCount} and {@code newCount} are today's totals; {@code items} is capped by the request's limit. */
public record DueResponse(int dueCount, int newCount, List<DueItemDto> items) {
}
