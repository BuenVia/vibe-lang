// Spaced repetition helpers shared by Study, Fill the gap and Review.
// Mirrors android/.../data/SettingsRepository.kt (thresholdMs) and ReviewReporter.kt.
import { useRef } from 'react';
import { api } from './api.js';

export const DEFAULT_ANSWER_SECONDS = 5;
export const DEFAULT_NEW_PER_DAY = 10;
export const FLASHCARD_MS = 3000; // flashcards: time from the card appearing to flipping it
export const EXTRA_GAP_MS = 2000; // each extra gap in a sentence adds this to the limit
export const ANSWER_SECONDS_OPTIONS = [3, 4, 5, 7, 10];
export const NEW_PER_DAY_OPTIONS = [0, 5, 10, 20, 30];

/** The speed limit to report with an answer, or null when the speed rule is off. */
export function thresholdMs(settings, kind, gaps = 1) {
  if (settings.speedRule === false) return null;
  if (kind === 'flashcard') return FLASHCARD_MS;
  const seconds = settings.answerSeconds ?? DEFAULT_ANSWER_SECONDS;
  return seconds * 1000 + EXTRA_GAP_MS * Math.max(0, gaps - 1);
}

export const isSlow = (ms, threshold) => ms != null && threshold != null && ms > threshold;
export const seconds = (ms) => `${(ms / 1000).toFixed(1)} s`;

/** The browser's IANA time zone, e.g. "Europe/Madrid", which the API uses to decide what "today" is. */
export function currentZone() {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

/** Best effort: a failed report is logged and otherwise ignored, so practice never stops. */
export function reportReview({ mode, vocabularyId = null, sentenceId = null, correct, responseMs, thresholdMs: limit, nearly = false }) {
  return api.reviews
    .report({
      vocabularyId,
      sentenceId,
      mode,
      correct,
      responseMs: Math.max(0, Math.round(responseMs)),
      thresholdMs: limit,
      zone: currentZone(),
      nearly: nearly || null,
    })
    .catch((e) => {
      console.warn("Couldn't report review", e);
      return null;
    });
}

/**
 * Times the current prompt and remembers which items were already reported in this session, so only the
 * first attempt at each one counts.
 */
export function usePromptTimer() {
  const shownAt = useRef(performance.now());
  const reported = useRef(new Set());
  return {
    restart: () => {
      shownAt.current = performance.now();
    },
    reset: () => {
      reported.current = new Set();
      shownAt.current = performance.now();
    },
    elapsedMs: () => performance.now() - shownAt.current,
    firstAttempt: (key) => {
      if (reported.current.has(key)) return false;
      reported.current.add(key);
      return true;
    },
  };
}

/** Shown on a correct answer that was too slow to move the item up in Review. */
export function SlowNote({ ms, threshold }) {
  if (!isSlow(ms, threshold)) return null;
  return (
    <div>
      But slow: {seconds(ms)} (limit {seconds(threshold)}). Answer faster to move it up in Review.
    </div>
  );
}
