import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading } from '../components/Status.jsx';
import { reportReview, thresholdMs, usePromptTimer } from '../reviews.jsx';

function shuffle(list) {
  const copy = [...list];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

/** Study: flashcards you flip, then mark as "Got it" or "Again". */
export default function StudyPage() {
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
  const timer = usePromptTimer();
  const flipMs = useRef(null); // time to the first flip of the current card

  const start = useCallback(
    (cards) => {
      setQueue(settings.shuffleCards ? shuffle(cards) : cards);
      setTotal(cards.length);
      setMissed(new Set());
      setFlipped(false);
      timer.reset();
      flipMs.current = null;
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [settings.shuffleCards],
  );

  useEffect(() => {
    if (data) start(data.vocabulary);
  }, [data, start]);

  const current = queue[0];

  const flip = useCallback(() => {
    setFlipped((f) => {
      if (!f && flipMs.current == null) flipMs.current = timer.elapsedMs();
      return !f;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** Reports the first answer for each card; the time counted is the time to flip it. */
  const report = (card, correct) => {
    if (!timer.firstAttempt(`v${card.id}`)) return;
    reportReview({
      mode: 'STUDY',
      vocabularyId: card.id,
      correct,
      responseMs: flipMs.current ?? timer.elapsedMs(),
      thresholdMs: thresholdMs(settings, 'flashcard'),
    });
  };

  const moveOn = () => {
    setFlipped(false);
    flipMs.current = null;
    timer.restart();
  };

  const gotIt = useCallback(() => {
    if (!current) return;
    report(current, true);
    setQueue((q) => q.slice(1));
    moveOn();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current, settings]);

  const again = useCallback(() => {
    if (!current) return;
    report(current, false);
    setMissed((m) => new Set(m).add(current.id));
    setQueue((q) => [...q.slice(1), q[0]]);
    moveOn();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current, settings]);

  useEffect(() => {
    function onKey(e) {
      // Let focused controls handle their own keys (avoids double actions on Space).
      if (['INPUT', 'SELECT', 'TEXTAREA', 'BUTTON'].includes(e.target.tagName)) return;
      if (e.code === 'Space') {
        e.preventDefault();
        flip();
      } else if (flipped && e.key === 'ArrowRight') gotIt();
      else if (flipped && e.key === 'ArrowLeft') again();
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [flipped, flip, gotIt, again]);

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
        <h1>Study</h1>
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
            onClick={flip}
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
              <button className="primary" onClick={flip}>
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
                Study missed cards
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
