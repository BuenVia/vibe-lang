package com.vibelang.review;

import com.vibelang.review.ReviewScheduler.Outcome;
import com.vibelang.review.ReviewScheduler.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    private static final int THRESHOLD = 5000;

    private static Result fast(int box) {
        return ReviewScheduler.schedule(box, NOW, false, true, 1200, THRESHOLD, NOW);
    }

    private static Instant days(int n) {
        return NOW.plus(Duration.ofDays(n));
    }

    @ParameterizedTest(name = "box {0} -> box {1}, due in {2} days")
    @CsvSource({"0,1,1", "1,2,2", "2,3,4", "3,4,8", "4,5,16", "5,5,32"})
    void fastCorrectAnswersPromote(int box, int newBox, int dueDays) {
        assertEquals(new Result(newBox, days(dueDays), Outcome.PROMOTED), fast(box));
    }

    @Test
    void slowCorrectAnswerKeepsTheBoxAndIsDueTomorrow() {
        assertEquals(new Result(3, days(1), Outcome.SLOW),
                ReviewScheduler.schedule(3, NOW, false, true, 5001, THRESHOLD, NOW));
    }

    @Test
    void answerExactlyOnTheThresholdCountsAsFast() {
        assertEquals(Outcome.PROMOTED, ReviewScheduler.schedule(3, NOW, false, true, 5000, THRESHOLD, NOW).outcome());
    }

    @Test
    void noThresholdSwitchesTheSpeedRuleOff() {
        assertEquals(Outcome.PROMOTED, ReviewScheduler.schedule(2, NOW, false, true, 60_000, null, NOW).outcome());
    }

    @Test
    void wrongAnswerGoesBackToBoxOneAndIsDueNow() {
        assertEquals(new Result(1, NOW, Outcome.LAPSED), ReviewScheduler.schedule(5, days(10), false, false, 900, THRESHOLD, NOW));
        assertEquals(new Result(1, NOW, Outcome.LAPSED), ReviewScheduler.schedule(0, null, false, false, 900, THRESHOLD, NOW));
    }

    @Test
    void secondCorrectAnswerTheSameDayDoesNotPromote() {
        // Wrong this morning (box 1, due now), right this afternoon: stays in box 1, comes back tomorrow.
        assertEquals(new Result(1, days(1), Outcome.SAME_DAY),
                ReviewScheduler.schedule(1, NOW, true, true, 800, THRESHOLD, NOW));
    }

    @Test
    void sameDayAnswerNeverBringsTheDueDateForward() {
        // Promoted earlier today to box 3 (due in 4 days); practising it again keeps that date.
        assertEquals(new Result(3, days(4), Outcome.SAME_DAY),
                ReviewScheduler.schedule(3, days(4), true, true, 800, THRESHOLD, NOW));
    }

    @Test
    void wrongAnswerTheSameDayStillLapses() {
        assertEquals(Outcome.LAPSED, ReviewScheduler.schedule(3, days(4), true, false, 800, THRESHOLD, NOW).outcome());
    }
}
