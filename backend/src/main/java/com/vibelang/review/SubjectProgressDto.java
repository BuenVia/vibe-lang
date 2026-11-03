package com.vibelang.review;

/**
 * Review progress of one subject. {@code boxes[i]} counts its items in box i; never-reviewed items count as box 0.
 * {@code due} is how many are due today.
 */
public record SubjectProgressDto(Long subjectId, int total, int[] boxes, int due) {

    /** Items in box 4 or 5: well known. */
    public int mastered() {
        return boxes[4] + boxes[5];
    }
}
