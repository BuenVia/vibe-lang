import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading } from '../components/Status.jsx';
import { Verdict, checkAnswer, clozeGaps, displayAnswer, hasGap, solvedSentence } from '../cloze.js';
import { ClozeSentence } from '../components/ClozeText.jsx';
import { SlowNote, reportReview, thresholdMs, usePromptTimer } from '../reviews.jsx';

function shuffle(list) {
  const copy = [...list];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

/**
 * Fill the gap: each sentence is shown with its gap(s) blanked out and you type the missing word(s).
 * A sentence with any wrong gap goes to the back of the queue until it is answered correctly.
 */
export default function ClozePage() {
  const { id } = useParams();
  const { settings } = useSettings();
  const { data, error, loading } = useLoad(async () => {
    const [subject, sentences] = await Promise.all([api.subjects.get(id), api.sentences.list(id)]);
    return { subject, sentences };
  }, [id]);

  const [queue, setQueue] = useState([]);
  const [total, setTotal] = useState(0);
  const [missed, setMissed] = useState(new Set());
  const [answers, setAnswers] = useState([]);
  const [verdicts, setVerdicts] = useState(null); // null until the current sentence is checked
  const [step, setStep] = useState(0); // increases on every new prompt, so the fields reset
  const [responseMs, setResponseMs] = useState(null);
  const timer = usePromptTimer();
  const firstInput = useRef(null);

  const start = useCallback(
    (sentences) => {
      const usable = sentences.filter((s) => hasGap(s.text));
      setQueue(settings.shuffleCards ? shuffle(usable) : usable);
      setTotal(usable.length);
      setMissed(new Set());
      setVerdicts(null);
      setStep((n) => n + 1);
      timer.reset();
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [settings.shuffleCards],
  );

  useEffect(() => {
    if (data) start(data.sentences);
  }, [data, start]);

  const current = queue[0];
  const gaps = current ? clozeGaps(current.text) : [];

  // Fresh, empty fields for each sentence (including when the same one comes straight back).
  useEffect(() => {
    setAnswers(gaps.map(() => ''));
    firstInput.current?.focus();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [step]);

  const complete = answers.length === gaps.length && answers.every((a) => a.trim());
  const limit = thresholdMs(settings, 'typed', gaps.length);
  const correct = verdicts?.every((v) => v !== Verdict.INCORRECT);

  const check = () => {
    if (!complete || verdicts) return;
    const ms = timer.elapsedMs();
    const result = gaps.map((expected, i) => checkAnswer(answers[i], expected));
    const ok = result.every((v) => v !== Verdict.INCORRECT);
    setVerdicts(result);
    setResponseMs(ms);
    if (!ok) setMissed((m) => new Set(m).add(current.id));
    if (timer.firstAttempt(`s${current.id}`)) {
      reportReview({ mode: 'CLOZE', sentenceId: current.id, correct: ok, responseMs: ms, thresholdMs: limit });
    }
  };

  const next = () => {
    setQueue((q) => (correct ? q.slice(1) : [...q.slice(1), q[0]]));
    setVerdicts(null);
    setStep((n) => n + 1);
    timer.restart();
  };

  const onSubmit = (e) => {
    e.preventDefault();
    if (verdicts) next();
    else check();
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage error={error} />;

  const { subject, sentences } = data;
  const done = total - queue.length;

  return (
    <section className="flashcards">
      <p className="breadcrumb">
        <Link to={`/subjects/${id}`}>← {subject.title}</Link>
      </p>
      <div className="page-header">
        <h1>Fill the gap</h1>
        <div className="actions">
          <button onClick={() => start(sentences)}>Restart</button>
        </div>
      </div>

      {total === 0 ? (
        <div className="empty">This subject has no sentences yet.</div>
      ) : current ? (
        <form onSubmit={onSubmit}>
          <div className="progress">
            <div className="progress-bar" style={{ width: `${(done / total) * 100}%` }} />
          </div>
          <p className="muted small center">
            {done} / {total} correct · {missed.size} missed
          </p>

          <div className="card cloze-card">
            <p className="cloze-sentence">
              <ClozeSentence text={current.text} verdicts={verdicts} />
            </p>
            {current.translation && <p className="cloze-translation">{current.translation}</p>}
            {current.hint && <p className="cloze-hint">({current.hint})</p>}
          </div>

          <div className="cloze-inputs">
            {gaps.map((_, i) => (
              <label key={i} className="field">
                <span>{gaps.length === 1 ? 'Missing word(s)' : `Gap ${i + 1}`}</span>
                <input
                  ref={i === 0 ? firstInput : undefined}
                  value={answers[i] ?? ''}
                  readOnly={!!verdicts}
                  autoComplete="off"
                  autoCapitalize="off"
                  spellCheck={false}
                  onChange={(e) => setAnswers((a) => a.map((v, j) => (j === i ? e.target.value : v)))}
                />
              </label>
            ))}
          </div>

          {verdicts && (
            <div className={`card cloze-feedback ${correct ? 'ok' : 'bad'}`}>
              <strong>
                {!correct
                  ? 'Not quite'
                  : verdicts.includes(Verdict.ACCENTS)
                    ? 'Correct, but watch the accents'
                    : 'Correct!'}
              </strong>
              {correct && <SlowNote ms={responseMs} threshold={limit} />}
              {gaps.map(
                (expected, i) =>
                  verdicts[i] !== Verdict.CORRECT && (
                    <div key={i}>
                      {gaps.length > 1 && `Gap ${i + 1}: `}you wrote “{answers[i].trim()}” → {displayAnswer(expected)}
                    </div>
                  ),
              )}
              {!correct && <div className="small">This sentence will come back at the end.</div>}
              {current.explanation && <div className="cloze-explanation">{current.explanation}</div>}
            </div>
          )}

          <div className="flashcard-actions">
            <button className="primary" type="submit" disabled={!verdicts && !complete}>
              {verdicts ? 'Next (Enter)' : 'Check (Enter)'}
            </button>
          </div>
        </form>
      ) : (
        <div className="card center">
          <h2>Done!</h2>
          <p>
            You got {total - missed.size} of {total} right first time.
          </p>
          {missed.size > 0 && (
            <ul className="plain-list cloze-review">
              {sentences
                .filter((s) => missed.has(s.id))
                .map((s) => (
                  <li key={s.id}>{solvedSentence(s.text)}</li>
                ))}
            </ul>
          )}
          <div className="flashcard-actions">
            {missed.size > 0 && (
              <button onClick={() => start(sentences.filter((s) => missed.has(s.id)))}>Practice missed sentences</button>
            )}
            <button className="primary" onClick={() => start(sentences)}>
              Start again
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
