package com.vibelang.review;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Picks what to review now from a course's items (given in learning-path order). Pure logic, easy to test.
 *
 * <ol>
 *   <li>Items that are due before the end of the learner's day, oldest due first.</li>
 *   <li>Then new items (never reviewed) in learning-path order, so new material comes in gradually: at most
 *       {@code newLimit} per day, counting the items already introduced today.</li>
 * </ol>
 */
public final class DueSelector {

    /** An item with its review state, or null when it has never been reviewed. */
    public record Candidate<T>(T item, ReviewState state) {
        boolean isNew() { return state == null; }
    }

    /** {@code dueCount} and {@code newCount} are the totals before {@code limit} is applied. */
    public record Selection<T>(List<Candidate<T>> items, int dueCount, int newCount) {}

    private DueSelector() {}

    public static <T> Selection<T> select(List<Candidate<T>> inLearningOrder, Instant now, ZoneId zone, int limit,
                                          int newLimit) {
        ZonedDateTime today = now.atZone(zone).toLocalDate().atStartOfDay(zone);
        Instant startOfToday = today.toInstant();
        Instant endOfToday = today.plusDays(1).toInstant();

        List<Candidate<T>> due = inLearningOrder.stream()
                .filter(c -> !c.isNew() && c.state().getDueAt().isBefore(endOfToday))
                .sorted(Comparator.comparing(c -> c.state().getDueAt()))
                .toList();

        long introducedToday = inLearningOrder.stream()
                .filter(c -> !c.isNew() && !c.state().getFirstReviewedAt().isBefore(startOfToday))
                .count();
        int newQuota = (int) Math.max(0, newLimit - introducedToday);
        List<Candidate<T>> fresh = inLearningOrder.stream().filter(Candidate::isNew).limit(newQuota).toList();

        List<Candidate<T>> items = new ArrayList<>(due);
        items.addAll(fresh);
        return new Selection<>(items.subList(0, Math.min(limit, items.size())), due.size(), fresh.size());
    }

    /** Whether {@code reviewedAt} falls on the same day as {@code now} in the learner's time zone. */
    public static boolean sameDay(Instant reviewedAt, Instant now, ZoneId zone) {
        return reviewedAt != null && reviewedAt.atZone(zone).toLocalDate().equals(now.atZone(zone).toLocalDate());
    }
}
