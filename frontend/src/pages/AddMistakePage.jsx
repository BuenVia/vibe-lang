import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading, NoCourse } from '../components/Status.jsx';
import { HighlightedSentence } from '../components/ClozeText.jsx';
import { hasGap, solvedSentence } from '../cloze.js';
import { markDifferences } from '../diff.js';

// Same names as the Android app, so both find the same subject.
const MISTAKES_CATEGORY = 'Mis errores';
const MISTAKES_SUBJECT = 'Errores';
const NEW_ERRORES = 'new-errores';

/** Finds or creates the "Mis errores" category and its "Errores" subject; returns the subject id. */
async function ensureErroresSubject(courseId) {
  const categories = await api.categories.list(courseId);
  const category =
    categories.find((c) => c.name === MISTAKES_CATEGORY) ??
    (await api.categories.create(courseId, { name: MISTAKES_CATEGORY, description: "Mistakes I've made, logged to practise." }));
  const subjects = await api.subjects.list(courseId);
  const subject =
    subjects.find((s) => s.categoryId === category.id && s.title === MISTAKES_SUBJECT) ??
    (await api.subjects.create({ categoryId: category.id, title: MISTAKES_SUBJECT, description: 'Sentences I got wrong in real life.' }));
  return subject.id;
}

/**
 * Log a real mistake: what I said, the corrected sentence with the fixed part as {{gaps}} ("Mark differences"
 * does that from a word diff), and where to file it. `?text=` prefills "What I said".
 */
export default function AddMistakePage() {
  const { course, settings, update } = useSettings();
  const courseId = course?.id;
  const [search] = useSearchParams();
  const navigate = useNavigate();
  const prefill = search.get('text') ?? '';
  const { data: subjects, error: loadError, loading } = useLoad(() => (courseId ? api.subjects.list(courseId) : []), [courseId]);

  const [wrong, setWrong] = useState(prefill);
  const [correct, setCorrect] = useState(prefill);
  const [touched, setTouched] = useState(false);
  const [translation, setTranslation] = useState('');
  const [note, setNote] = useState('');
  const [target, setTarget] = useState('');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const erroresExists = subjects?.some((s) => s.title === MISTAKES_SUBJECT && s.categoryName === MISTAKES_CATEGORY);

  // Default: the subject used last time, else "Errores".
  useEffect(() => {
    if (!subjects || target) return;
    const last = subjects.find((s) => s.id === settings.lastMistakeSubjectId);
    const errores = subjects.find((s) => s.title === MISTAKES_SUBJECT && s.categoryName === MISTAKES_CATEGORY);
    setTarget(String(last?.id ?? errores?.id ?? NEW_ERRORES));
  }, [subjects, target, settings.lastMistakeSubjectId]);

  if (!course) return <NoCourse />;
  if (loading) return <Loading />;
  if (loadError) return <ErrorMessage error={loadError} />;

  const mark = () => {
    const marked = markDifferences(wrong, solvedSentence(correct));
    setTouched(true);
    if (hasGap(marked)) {
      setCorrect(marked);
      setMessage(null);
    } else {
      setMessage('No differences found. Fix the sentence first, or add {{…}} yourself.');
    }
  };

  const save = async (e) => {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const subjectId = target === NEW_ERRORES ? await ensureErroresSubject(courseId) : Number(target);
      await api.sentences.create(subjectId, {
        text: correct.trim(),
        translation: translation.trim() || null,
        explanation: note.trim() || null,
        source: 'MISTAKE',
        wrongVersion: wrong.trim(),
      });
      update({ lastMistakeSubjectId: subjectId });
      navigate('/mistakes');
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  const canSave = wrong.trim() && hasGap(correct) && target && !saving;

  return (
    <section className="narrow">
      <h1>Add mistake</h1>
      <p className="muted">
        Write what you said, then the corrected sentence. Put the corrected words in <code>{'{{…}}'}</code>, or let “Mark
        differences” do it. It becomes a sentence for Fill the gap, Translate and Review.
      </p>
      <ErrorMessage error={error} />
      <form className="form card" onSubmit={save}>
        <label className="field">
          <span>What I said</span>
          <input
            value={wrong}
            onChange={(e) => {
              setWrong(e.target.value);
              if (!touched) setCorrect(e.target.value);
            }}
          />
        </label>
        <label className="field">
          <span>Correct version</span>
          <input
            value={correct}
            onChange={(e) => {
              setCorrect(e.target.value);
              setTouched(true);
              setMessage(null);
            }}
          />
          <span className={`small ${message ? 'error-text' : 'muted'}`}>
            {message ?? 'Mark the corrected part with {{…}}; use / for alternatives.'}
          </span>
        </label>
        <div>
          <button type="button" onClick={mark} disabled={!wrong.trim() || !correct.trim()}>
            Mark differences
          </button>
        </div>
        {hasGap(correct) && (
          <div className="muted">
            Preview: <HighlightedSentence text={correct} />
          </div>
        )}
        <label className="field">
          <span>File it under</span>
          <select value={target} onChange={(e) => setTarget(e.target.value)}>
            {!erroresExists && <option value={NEW_ERRORES}>{MISTAKES_SUBJECT} (new subject)</option>}
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.title}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Translation (optional, enables Translate)</span>
          <input value={translation} onChange={(e) => setTranslation(e.target.value)} />
        </label>
        <label className="field">
          <span>Note, e.g. the rule (optional)</span>
          <input value={note} onChange={(e) => setNote(e.target.value)} />
        </label>
        <div>
          <button className="primary" disabled={!canSave}>
            {saving ? 'Saving…' : 'Save mistake'}
          </button>
        </div>
      </form>
    </section>
  );
}
