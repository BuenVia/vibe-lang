package com.vibelang.review;

import com.vibelang.review.DueSelector.Candidate;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DueSelectorTest {

    // 10:00 in Madrid (UTC+1 in November).
    private static final Instant NOW = Instant.parse("2026-11-03T09:00:00Z");
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private static ReviewState state(Instant due, Instant firstReviewed) {
        ReviewState s = new ReviewState();
        s.setBox(2);
        s.setDueAt(due);
        s.setFirstReviewedAt(firstReviewed);
        return s;
    }

    private static Candidate<String> reviewed(String name, Instant due) {
        return new Candidate<>(name, state(due, NOW.minus(Duration.ofDays(30))));
    }

    private static Candidate<String> fresh(String name) {
        return new Candidate<>(name, null);
    }

    private static List<String> names(DueSelector.Selection<String> s) {
        return s.items().stream().map(Candidate::item).toList();
    }

    @Test
    void dueItemsComeFirstOldestFirstThenNewItemsInLearningOrder() {
        var selection = DueSelector.select(List.of(
                fresh("new-1"),
                reviewed("due-yesterday", NOW.minus(Duration.ofDays(1))),
                reviewed("not-due", NOW.plus(Duration.ofDays(3))),
                fresh("new-2"),
                reviewed("due-last-week", NOW.minus(Duration.ofDays(7)))), NOW, MADRID, 50, 10);

        assertEquals(List.of("due-last-week", "due-yesterday", "new-1", "new-2"), names(selection));
        assertEquals(2, selection.dueCount());
        assertEquals(2, selection.newCount());
    }

    @Test
    void itemsDueLaterTodayCountAsDue() {
        // 23:30 Madrid time today is still today; 00:30 tomorrow is not.
        var selection = DueSelector.select(List.of(
                reviewed("tonight", Instant.parse("2026-11-03T22:30:00Z")),
                reviewed("after-midnight", Instant.parse("2026-11-03T23:30:00Z"))), NOW, MADRID, 50, 10);
        assertEquals(List.of("tonight"), names(selection));
    }

    @Test
    void theDayBoundaryFollowsTheLearnersZone() {
        Candidate<String> c = reviewed("x", Instant.parse("2026-11-03T23:30:00Z"));
        assertEquals(1, DueSelector.select(List.of(c), NOW, ZoneOffset.UTC, 50, 10).dueCount());
        assertEquals(0, DueSelector.select(List.of(c), NOW, MADRID, 50, 10).dueCount());
    }

    @Test
    void newItemsAreCappedPerDayIncludingThoseAlreadyIntroducedToday() {
        Candidate<String> introducedToday = new Candidate<>("today", state(NOW.plus(Duration.ofDays(1)), NOW.minusSeconds(60)));
        var selection = DueSelector.select(List.of(introducedToday, fresh("a"), fresh("b"), fresh("c")), NOW, MADRID, 50, 3);
        assertEquals(List.of("a", "b"), names(selection));
        assertEquals(2, selection.newCount());

        assertEquals(0, DueSelector.select(List.of(fresh("a")), NOW, MADRID, 50, 0).newCount());
    }

    @Test
    void limitCapsTheItemsButNotTheCounts() {
        var selection = DueSelector.select(List.of(
                reviewed("d1", NOW.minusSeconds(30)), reviewed("d2", NOW.minusSeconds(20)), fresh("n1")), NOW, MADRID, 2, 10);
        assertEquals(List.of("d1", "d2"), names(selection));
        assertEquals(2, selection.dueCount());
        assertEquals(1, selection.newCount());
    }

    @Test
    void sameDayUsesTheLearnersZone() {
        Instant lateLastNightMadrid = Instant.parse("2026-11-02T22:30:00Z"); // 23:30 on the 2nd in Madrid
        assertFalse(DueSelector.sameDay(lateLastNightMadrid, NOW, MADRID));
        assertTrue(DueSelector.sameDay(NOW.minusSeconds(3600), NOW, MADRID));
        assertFalse(DueSelector.sameDay(null, NOW, MADRID));
    }
}
