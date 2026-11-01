import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading } from '../components/Status.jsx';

function shuffle(list) {
  const copy = [...list];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

export default function FlashcardsPage() {
  const { id } = useParams();
  const { settings, update } = useSettings();
  const { data, error, loading } = useLoad(async () => {
    const [subject, vocabulary] = await Promise.all([api.subjects.get(id), api.vocabulary.list(id)]);
    return { subject, vocabulary };
  }, [id]);

  const [queue, setQueue] = useState([]);
  const [flipped, setFlipped] = useState(false);
  const [missed, setMissed] = useState(new Set());
  const [total, setTotal] = useState(0);

  const start = useCallback(
    (cards) => {
      setQueue(settings.shuffleCards ? shuffle(cards) : cards);
      setTotal(cards.length);
      setMissed(new Set());
      setFlipped(false);
    },
    [settings.shuffleCards],
  );

  useEffect(() => {
    if (data) start(data.vocabulary);
  }, [data, start]);

  const current = queue[0];

  const gotIt = useCallback(() => {
    setQueue((q) => q.slice(1));
    setFlipped(false);
  }, []);

  const again = useCallback(() => {
    if (!current) return;
    setMissed((m) => new Set(m).add(current.id));
    setQueue((q) => [...q.slice(1), q[0]]);
    setFlipped(false);
  }, [current]);

  useEffect(() => {
    function onKey(e) {
      // Let focused controls handle their own keys (avoids double actions on Space).
      if (['INPUT', 'SELECT', 'TEXTAREA', 'BUTTON'].includes(e.target.tagName)) return;
      if (e.code === 'Space') {
        e.preventDefault();
        setFlipped((f) => !f);
      } else if (flipped && e.key === 'ArrowRight') gotIt();
      else if (flipped && e.key === 'ArrowLeft') again();
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [flipped, gotIt, again]);

  if (loading) return <Loading />;
  if (error) return <ErrorMessage error={error} />;

  const { subject, vocabulary } = data;
  const termFirst = settings.cardFront === 'term';
  const done = total - queue.length;

  return (
    <section className="flashcards">
      <p className="breadcrumb">
        <Link to={`/subjects/${id}`}>← {subject.title}</Link>
      </p>
      <div className="page-header">
        <h1>Flashcards</h1>
        <div className="actions">
          <label className="inline">
            Front:
            <select value={settings.cardFront} onChange={(e) => update({ cardFront: e.target.value })}>
              <option value="term">Term</option>
              <option value="translation">Translation</option>
            </select>
          </label>
          <button onClick={() => start(vocabulary)}>Restart</button>
        </div>
      </div>

      {vocabulary.length === 0 ? (
        <div className="empty">This subject has no vocabulary yet.</div>
      ) : current ? (
        <>
          <div className="progress">
            <div className="progress-bar" style={{ width: `${(done / total) * 100}%` }} />
          </div>
          <p className="muted small center">
            {done} / {total} learned · {missed.size} missed
          </p>

          <div
            className={`flashcard ${flipped ? 'flipped' : ''}`}
            onClick={() => setFlipped((f) => !f)}
            role="button"
            tabIndex={0}
            aria-label="Flip card"
          >
            <span className="flashcard-inner">
              <span className="flashcard-face front">
                <span className="flashcard-text">{termFirst ? current.term : current.translation}</span>
              </span>
              <span className="flashcard-face back">
                <span className="flashcard-text">{termFirst ? current.translation : current.term}</span>
                {current.notes && <span className="flashcard-notes">{current.notes}</span>}
              </span>
            </span>
          </div>

          <div className="flashcard-actions">
            {flipped ? (
              <>
                <button className="danger" onClick={again}>
                  ← Again
                </button>
                <button className="success" onClick={gotIt}>
                  Got it →
                </button>
              </>
            ) : (
              <button className="primary" onClick={() => setFlipped(true)}>
                Show answer (space)
              </button>
            )}
          </div>
        </>
      ) : (
        <div className="card center">
          <h2>Done!</h2>
          <p>
            You went through {total} cards
            {missed.size > 0 ? ` and missed ${missed.size} at least once.` : ' without a single miss.'}
          </p>
          <div className="flashcard-actions">
            {missed.size > 0 && (
              <button onClick={() => start(vocabulary.filter((v) => missed.has(v.id)))}>
                Practise missed cards
              </button>
            )}
            <button className="primary" onClick={() => start(vocabulary)}>
              Start again
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
