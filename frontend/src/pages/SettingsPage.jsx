import { courseLabel, useSettings } from '../context/SettingsContext.jsx';
import { ANSWER_SECONDS_OPTIONS, EXTRA_GAP_MS, FLASHCARD_MS, NEW_PER_DAY_OPTIONS } from '../reviews.jsx';

export default function SettingsPage() {
  const { settings, update, courses } = useSettings();

  return (
    <section className="narrow">
      <h1>Settings</h1>

      <div className="card">
        <h2>Mode</h2>
        <label className="radio">
          <input
            type="radio"
            name="mode"
            checked={settings.mode === 'learner'}
            onChange={() => update({ mode: 'learner' })}
          />
          <span>
            <strong>Learner</strong>
            <span className="muted"> — read lessons and study flashcards. Editing is hidden.</span>
          </span>
        </label>
        <label className="radio">
          <input
            type="radio"
            name="mode"
            checked={settings.mode === 'admin'}
            onChange={() => update({ mode: 'admin' })}
          />
          <span>
            <strong>Admin</strong>
            <span className="muted"> — create, edit and delete courses, categories, subjects, lessons and vocabulary.</span>
          </span>
        </label>
      </div>

      <div className="card">
        <h2>Course</h2>
        {courses.length === 0 ? (
          <p className="muted">No courses yet.</p>
        ) : (
          <select value={settings.courseId ?? ''} onChange={(e) => update({ courseId: Number(e.target.value) })}>
            {courses.map((c) => (
              <option key={c.id} value={c.id}>
                {courseLabel(c)}
              </option>
            ))}
          </select>
        )}
      </div>

      <div className="card">
        <h2>Study</h2>
        <label className="field">
          <span>Show on the front of the card</span>
          <select value={settings.cardFront} onChange={(e) => update({ cardFront: e.target.value })}>
            <option value="term">Term (e.g. Spanish)</option>
            <option value="translation">Translation (e.g. English)</option>
          </select>
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={settings.shuffleCards}
            onChange={(e) => update({ shuffleCards: e.target.checked })}
          />
          Shuffle cards
        </label>
      </div>

      <div className="card">
        <h2>Review</h2>
        <p className="muted small">
          Every answer in Study, Fill the gap and Review moves the item through boxes 1–5. Wrong answers go back to box
          1; right answers move up a box and come back after 1, 2, 4, 8 or 16 days.
        </p>
        <label className="checkbox">
          <input type="checkbox" checked={settings.speedRule} onChange={(e) => update({ speedRule: e.target.checked })} />
          <span>
            Speed rule
            <span className="muted">
              {' '}
              — only move an item up when you answer within the time limit (flashcards: flip within{' '}
              {FLASHCARD_MS / 1000} s).
            </span>
          </span>
        </label>
        {settings.speedRule && (
          <label className="field">
            <span>Time limit for typed answers</span>
            <select value={settings.answerSeconds} onChange={(e) => update({ answerSeconds: Number(e.target.value) })}>
              {ANSWER_SECONDS_OPTIONS.map((n) => (
                <option key={n} value={n}>
                  {n} seconds (+{EXTRA_GAP_MS / 1000} s per extra gap)
                </option>
              ))}
            </select>
          </label>
        )}
        <label className="field">
          <span>New items per day</span>
          <select value={settings.newPerDay} onChange={(e) => update({ newPerDay: Number(e.target.value) })}>
            {NEW_PER_DAY_OPTIONS.map((n) => (
              <option key={n} value={n}>
                {n === 0 ? "None (only review what's due)" : n}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="card">
        <h2>Speaking</h2>
        <p className="muted small">
          Fill the gap and the sentences in Review have a 🎤 button: say the whole sentence and the gaps are checked
          against what was heard (Chrome and Edge; the browser sends the audio to its speech service). 🔊 reads the
          Spanish aloud.
        </p>
        <label className="checkbox">
          <input type="checkbox" checked={settings.readAloud} onChange={(e) => update({ readAloud: e.target.checked })} />
          <span>
            Read answers aloud<span className="muted"> — after each Check, hear the correct Spanish.</span>
          </span>
        </label>
      </div>
    </section>
  );
}
