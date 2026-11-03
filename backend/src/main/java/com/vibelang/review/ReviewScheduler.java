package com.vibelang.review;

import java.time.Duration;
import java.time.Instant;

/**
 * Leitner scheduling with a speed rule. Pure logic, no Spring, so it is easy to test.
 *
 * <ul>
 *   <li><b>Wrong:</b> back to box 1 and due again straight away.</li>
 *   <li><b>Correct, but already reviewed earlier today:</b> no promotion (so cramming the same item several times
 *       in a day can't move it up); the box stays and it is due again tomorrow at the earliest.</li>
 *   <li><b>Correct but slower than the threshold:</b> the box stays and it is due again tomorrow.</li>
 *   <li><b>Correct and fast:</b> up one box (max {@value #MAX_BOX}) and due after that box's interval: 1, 2, 4, 8
 *       or 16 days, and 32 days for an item that was already in the top box.</li>
 * </ul>
 * A null threshold switches the speed rule off: every correct answer counts as fast. A "nearly" answer (a
 * self-graded small slip, from Translate) is correct but never promotes: the box stays and it is due tomorrow.
 */
public final class ReviewScheduler {

    public static final int MAX_BOX = 5;
    private static final Duration ONE_DAY = Duration.ofDays(1);

    public enum Outcome { PROMOTED, SLOW, NEARLY, SAME_DAY, LAPSED }

    public record Result(int box, Instant dueAt, Outcome outcome) {}

    private ReviewScheduler() {}

    /**
     * @param box           the current box (0 = never reviewed)
     * @param dueAt         when the item is currently due (null for a new item)
     * @param reviewedToday whether the item was already reviewed earlier on the learner's current day
     */
    public static Result schedule(int box, Instant dueAt, boolean reviewedToday, boolean correct, long responseMs,
                                  Integer thresholdMs, Instant now) {
        return schedule(box, dueAt, reviewedToday, correct, false, responseMs, thresholdMs, now);
    }

    public static Result schedule(int box, Instant dueAt, boolean reviewedToday, boolean correct, boolean nearly,
                                  long responseMs, Integer thresholdMs, Instant now) {
        if (!correct) {
            return new Result(1, now, Outcome.LAPSED);
        }
        if (reviewedToday) {
            Instant tomorrow = now.plus(ONE_DAY);
            Instant due = dueAt != null && dueAt.isAfter(tomorrow) ? dueAt : tomorrow;
            return new Result(box, due, Outcome.SAME_DAY);
        }
        if (nearly) {
            return new Result(box, now.plus(ONE_DAY), Outcome.NEARLY);
        }
        if (thresholdMs != null && responseMs > thresholdMs) {
            return new Result(box, now.plus(ONE_DAY), Outcome.SLOW);
        }
        int newBox = Math.min(box + 1, MAX_BOX);
        int days = box >= MAX_BOX ? 32 : 1 << (newBox - 1);
        return new Result(newBox, now.plus(Duration.ofDays(days)), Outcome.PROMOTED);
    }
}
