import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { courseLabel, useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading, NoCourse } from '../components/Status.jsx';
import { ClozeSentence } from '../components/ClozeText.jsx';
import { Verdict, bestSpoken, checkAnswer, clozeGaps, displayAnswer, hasGap, solvedSentence, spokenAnswers } from '../cloze.js';
import { ListenButton, MicButton, speechLang, useSpeech } from '../speech.jsx';
import { DEFAULT_NEW_PER_DAY, SlowNote, currentZone, reportReview, thresholdMs, usePromptTimer } from '../reviews.jsx';

/** Turns the API's due items into prompts: a word (type the translation) or a sentence (fill the gaps). */
function toItems(due) {
  return due.items
    .map((d) =>
      d.type === 'VOCABULARY' && d.vocabulary
        ? { key: `v${d.vocabulary.id}`, word: d.vocabulary, expected: [d.vocabulary.translation], ...d }
        : d.type === 'SENTENCE' && d.sentence && hasGap(d.sentence.text)
          ? { key: `s${d.sentence.id}`, sentence: d.sentence, expected: clozeGaps(d.sentence.text), ...d }
          : null,
    )
    .filter(Boolean);
}

/**
 * Review: spaced repetition across the whole course. Due items first (oldest first), then a few new ones.
 * A wrong item goes to the back of the queue until it is right; only the first attempt is reported.
 */
export default function ReviewPage() {
  const { course, settings } = useSettings();
  const courseId = course?.id;
  const newLimit = settings.newPerDay ?? DEFAULT_NEW_PER_DAY;
  const [version, setVersion] = useState(0); // bump to fetch the due list again
  const { data: due, error, loading } = useLoad(
    () => (courseId ? api.reviews.due(courseId, { newLimit, zone: currentZone() }) : null),
    [courseId, newLimit, version],
  );

  const [queue, setQueue] = useState([]);
  const [total, setTotal] = useState(0);
  const [missed, setMissed] = useState(new Set());
  const [answers, setAnswers] = useState([]);
  const [verdicts, setVerdicts] = useState(null);
  const [responseMs, setResponseMs] = useState(null);
  const [step, setStep] = useState(0);
  const [heard, setHeard] = useState(null);
  const timer = usePromptTimer();
  const speech = useSpeech(speechLang(course?.language));
  const firstInput = useRef(null);

  const start = useCallback((d) => {
    const items = toItems(d);
    setQueue(items);
    setTotal(items.length);
    setMissed(new Set());
    setVerdicts(null);
    setStep((n) => n + 1);
    timer.reset();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (due) start(due);
  }, [due, start]);

  const current = queue[0];
  const expected = current?.expected ?? [];

  useEffect(() => {
    setAnswers(expected.map(() => ''));
    firstInput.current?.focus();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [step]);

  const limit = thresholdMs(settings, 'typed', expected.length);
  const complete = answers.length === expected.length && answers.every((a) => a.trim());
  const correct = verdicts?.every((v) => v !== Verdict.INCORRECT);

  // The Spanish side: the term for a word, the full sentence for a gap.
  const spanish = current ? (current.word ? current.word.term : solvedSentence(current.sentence.text)) : '';

  /** Shared by typed and spoken answers. */
  const finishCheck = (result, ms, mode) => {
    const ok = result.every((v) => v !== Verdict.INCORRECT);
    setVerdicts(result);
    setResponseMs(ms);
    if (!ok) setMissed((m) => new Set(m).add(current.key));
    if (timer.firstAttempt(current.key)) {
      reportReview({
        mode,
        vocabularyId: current.word?.id ?? null,
        sentenceId: current.sentence?.id ?? null,
        correct: ok,
        responseMs: ms,
        thresholdMs: limit,
      });
    }
    if (settings.readAloud) speech.speak(spanish);
  };

  const check = () => {
    if (!complete || verdicts) return;
    const ms = timer.elapsedMs();
    setHeard(null);
    finishCheck(expected.map((e, i) => checkAnswer(answers[i], e)), ms, 'REVIEW');
  };

  const checkSpoken = (transcripts) => {
    if (verdicts || !current?.sentence) return;
    const best = bestSpoken(expected, transcripts);
    if (!best) return;
    setHeard(best.heard);
    setAnswers(spokenAnswers(expected, best.verdicts));
    finishCheck(best.verdicts, timer.elapsedMs(), 'SPEAK');
  };

  const next = () => {
    setQueue((q) => (correct ? q.slice(1) : [...q.slice(1), q[0]]));
    setVerdicts(null);
    setHeard(null);
    setStep((n) => n + 1);
    timer.restart();
  };

  const onSubmit = (e) => {
    e.preventDefault();
    if (verdicts) next();
    else check();
  };

  if (!course) return <NoCourse />;
  if (loading) return <Loading />;
  if (error) return <ErrorMessage error={error} />;

  const done = total - queue.length;

  return (
    <section className="flashcards">
      <p className="breadcrumb">
        <Link to="/">← {courseLabel(course)}</Link>
      </p>
      <div className="page-header">
        <h1>Review</h1>
      </div>

      {total === 0 ? (
        <div className="empty">
          <p>
            <strong>All caught up!</strong>
          </p>
          <p>Nothing is due and there are no new items for today. Come back tomorrow, or raise “New items per day” in Settings.</p>
        </div>
      ) : current ? (
        <form onSubmit={onSubmit}>
          <div className="progress">
            <div className="progress-bar" style={{ width: `${(done / total) * 100}%` }} />
          </div>
          <p className="muted small center">
            {done} / {total} done · {missed.size} missed
          </p>

          <div className="card cloze-card">
            <div className="review-meta">
              <span className="tag">{current.subjectTitle}</span>
              <span className="muted small">{current.box === 0 ? 'New' : `Box ${current.box}`}</span>
            </div>
            {current.word ? (
              <p className="cloze-sentence">
                <strong>{current.word.term}</strong>
              </p>
            ) : (
              <>
                <p className="cloze-sentence">
                  <ClozeSentence text={current.sentence.text} verdicts={verdicts} />
                </p>
                {current.sentence.translation && <p className="cloze-translation">{current.sentence.translation}</p>}
                {current.sentence.hint && <p className="cloze-hint">({current.sentence.hint})</p>}
              </>
            )}
          </div>

          {!verdicts && current.sentence && (
            <div className="speech-actions">
              <MicButton speech={speech} onResult={checkSpoken} />
            </div>
          )}

          <div className="cloze-inputs">
            {expected.map((_, i) => (
              <label key={i} className="field">
                <span>{current.word ? 'Translation' : expected.length === 1 ? 'Missing word(s)' : `Gap ${i + 1}`}</span>
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
                {!correct ? 'Not quite' : verdicts.includes(Verdict.ACCENTS) ? 'Correct, but watch the accents' : 'Correct!'}
              </strong>
              {correct && <SlowNote ms={responseMs} threshold={limit} />}
              {heard && <div className="cloze-explanation">Heard: “{heard}”</div>}
              {expected.map(
                (e, i) =>
                  verdicts[i] !== Verdict.CORRECT && (
                    <div key={i}>
                      {expected.length > 1 && `Gap ${i + 1}: `}
                      {heard ? '' : `you wrote “${answers[i].trim()}” → `}
                      {displayAnswer(e)}
                    </div>
                  ),
              )}
              {!correct && <div className="small">This will come back at the end.</div>}
              {(current.word?.notes || current.sentence?.explanation) && (
                <div className="cloze-explanation">{current.word?.notes ?? current.sentence.explanation}</div>
              )}
              <div>
                <ListenButton speech={speech} text={spanish} />
              </div>
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
          <h2>Review done!</h2>
          <p>
            You got {total - missed.size} of {total} right first time. Missed items are due again today; correct ones
            come back after their interval.
          </p>
          <div className="flashcard-actions">
            <button onClick={() => setVersion((v) => v + 1)}>Check for more</button>
            <Link className="button primary" to="/">
              Back to the path
            </Link>
          </div>
        </div>
      )}
    </section>
  );
}
