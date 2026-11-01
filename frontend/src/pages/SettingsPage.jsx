import { courseLabel, useSettings } from '../context/SettingsContext.jsx';

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
            checked={settings.mode === 'study'}
            onChange={() => update({ mode: 'study' })}
          />
          <span>
            <strong>Study</strong>
            <span className="muted"> — read lessons and practise flashcards. Editing is hidden.</span>
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
        <h2>Flashcards</h2>
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
    </section>
  );
}
