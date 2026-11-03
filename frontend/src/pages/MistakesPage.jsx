import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading, NoCourse } from '../components/Status.jsx';
import { HighlightedSentence } from '../components/ClozeText.jsx';
import { solvedSentence } from '../cloze.js';

/** The mistakes log, newest first, with a subject filter and a link to practise them with Fill the gap. */
export default function MistakesPage() {
  const { course } = useSettings();
  const courseId = course?.id;
  const [filter, setFilter] = useState('');
  const [error, setError] = useState(null);
  const { data, error: loadError, loading, setData } = useLoad(async () => {
    if (!courseId) return null;
    const [mistakes, subjects] = await Promise.all([api.sentences.byCourse(courseId, 'MISTAKE'), api.subjects.list(courseId)]);
    return { mistakes, subjects: Object.fromEntries(subjects.map((s) => [s.id, s])) };
  }, [courseId]);

  if (!course) return <NoCourse />;
  if (loading) return <Loading />;
  if (loadError) return <ErrorMessage error={loadError} />;

  const { mistakes, subjects } = data;
  const subjectIds = [...new Set(mistakes.map((m) => m.subjectId))];
  const shown = mistakes.filter((m) => !filter || String(m.subjectId) === filter);

  const remove = async (m) => {
    if (!window.confirm(`Delete this mistake?\n\n${solvedSentence(m.text)}`)) return;
    try {
      await api.sentences.remove(m.id);
      setData((d) => ({ ...d, mistakes: d.mistakes.filter((x) => x.id !== m.id) }));
    } catch (e) {
      setError(e);
    }
  };

  return (
    <section className="narrow">
      <div className="page-header">
        <div>
          <h1>My mistakes</h1>
          <p className="muted">Corrections you've logged. They also appear in Fill the gap, Translate and Review.</p>
        </div>
        <Link className="button primary" to="/mistakes/new">
          Add mistake
        </Link>
      </div>
      <ErrorMessage error={error} />

      {mistakes.length === 0 ? (
        <div className="empty">No mistakes logged yet. When someone corrects you, add it here.</div>
      ) : (
        <>
          <div className="toolbar">
            <select value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Subject">
              <option value="">All subjects</option>
              {subjectIds.map((id) => (
                <option key={id} value={id}>
                  {subjects[id]?.title ?? `Subject ${id}`}
                </option>
              ))}
            </select>
            <Link className="button" to={`/mistakes/practice${filter ? `?subject=${filter}` : ''}`}>
              Practise {shown.length}
            </Link>
          </div>
          <ul className="plain-list mistakes">
            {shown.map((m) => (
              <li key={m.id}>
                <div>
                  {m.wrongVersion && (
                    <div>
                      <s className="error-text">{m.wrongVersion}</s>
                    </div>
                  )}
                  <div className="mistake-correct">
                    <HighlightedSentence text={m.text} />
                  </div>
                  {m.explanation && <div className="muted small">{m.explanation}</div>}
                  {subjects[m.subjectId] && <span className="tag">{subjects[m.subjectId].title}</span>}
                </div>
                <button className="danger" onClick={() => remove(m)}>
                  Delete
                </button>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}
