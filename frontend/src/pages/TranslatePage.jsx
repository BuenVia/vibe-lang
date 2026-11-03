import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading } from '../components/Status.jsx';
import { Verdict, expansions, hasGap, solvedSentence } from '../cloze.js';
import { checkSentence, closest } from '../diff.js';
import { reportReview, usePromptTimer } from '../reviews.jsx';
import { ListenButton, MicButton, speechLang, useSpeech } from '../speech.jsx';

function shuffle(list) {
  const copy = [...list];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

const usable = (sentences) => sentences.filter((s) => s.translation && hasGap(s.text));

/**
 * Translate: see the English and write (or say) the whole Spanish sentence. An exact match with any accepted
 * sentence is right straight away; otherwise compare with the closest model and grade it Right / Nearly / Wrong.
 */
export default function TranslatePage() {
  const { id } = useParams();
  const { settings, courses } = useSettings();
  const { data, error, loading } = useLoad(async () => {
    const [subject, sentences] = await Promise.all([api.subjects.get(id), api.sentences.list(id)]);
    return { subject, sentences };
  }, [id]);

  const [queue, setQueue] = useState([]);
  const [total, setTotal] = useState(0);
  const [missed, setMissed] = useState(new Set());
  const [answer, setAnswer] = useState('');
  const [feedback, setFeedback] = useState(null); // { model, diff, exact, ms }
  const [grade, setGrade] = useState(null); // 'RIGHT' | 'NEARLY' | 'WRONG'
  const timer = usePromptTimer();
  const input = useRef(null);
  const language = courses.find((c) => c.id === data?.subject.courseId)?.language;
  const speech = useSpeech(speechLang(language));

  const start = useCallback(
    (sentences) => {
      const items = usable(sentences);
      setQueue(settings.shuffleCards ? shuffle(items) : items);
      setTotal(items.length);
      setMissed(new Set());
      setFeedback(null);
      setGrade(null);
      setAnswer('');
      timer.reset();
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [settings.shuffleCards],
  );

  useEffect(() => {
    if (data) start(data.sentences);
  }, [data, start]);

  const current = queue[0];
  useEffect(() => {
    input.current?.focus();
  }, [current, queue.length]);

  const report = (g, ms) => {
    if (!timer.firstAttempt(`s${current.id}`)) return;
    reportReview({
      mode: 'TRANSLATE',
      sentenceId: current.id,
      correct: g !== 'WRONG',
      nearly: g === 'NEARLY',
      responseMs: ms,
      thresholdMs: null, // no speed limit for a whole sentence
    });
  };

  const check = () => {
    if (!current || feedback || !answer.trim()) return;
    const ms = timer.elapsedMs();
    const models = expansions(current.text);
    const exact = checkSentence(answer, models);
    const best = closest(answer, models);
    setFeedback({ ...best, exact, ms });
    if (exact !== Verdict.INCORRECT) {
      setGrade('RIGHT');
      report('RIGHT', ms);
    }
    if (settings.readAloud) speech.speak(best.model);
  };

  const chooseGrade = (g) => {
    if (grade) return;
    setGrade(g);
    if (g === 'WRONG') setMissed((m) => new Set(m).add(current.id));
    report(g, feedback.ms);
  };

  const next = () => {
    setQueue((q) => (grade === 'WRONG' ? [...q.slice(1), q[0]] : q.slice(1)));
    setFeedback(null);
    setGrade(null);
    setAnswer('');
    timer.restart();
  };

  const onSubmit = (e) => {
    e.preventDefault();
    if (!feedback) check();
    else if (grade) next();
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage error={error} />;

  const { subject, sentences } = data;
  const done = total - queue.length;
  const good = feedback && (feedback.exact !== Verdict.INCORRECT || (grade && grade !== 'WRONG'));

  return (
    <section className="flashcards">
      <p className="breadcrumb">
        <Link to={`/subjects/${id}`}>← {subject.title}</Link>
      </p>
      <div className="page-header">
        <h1>Translate</h1>
        <div className="actions">
          <button onClick={() => start(sentences)}>Restart</button>
        </div>
      </div>

      {total === 0 ? (
        <div className="empty">No sentences to translate yet. Translate uses the subject's sentences that have a translation.</div>
      ) : current ? (
        <form onSubmit={onSubmit}>
          <div className="progress">
            <div className="progress-bar" style={{ width: `${(done / total) * 100}%` }} />
          </div>
          <p className="muted small center">
            {done} / {total} done · {missed.size} missed
          </p>

          <div className="card cloze-card">
            <p className="cloze-sentence">{current.translation}</p>
          </div>

          <label className="field">
            <span>In Spanish</span>
            <textarea
              ref={input}
              rows={2}
              value={answer}
              readOnly={!!feedback}
              spellCheck={false}
              onChange={(e) => setAnswer(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) onSubmit(e);
              }}
            />
          </label>

          {!feedback && (
            <div className="speech-actions" style={{ marginTop: '0.75rem' }}>
              <MicButton speech={speech} label="Say it" onResult={(guesses) => setAnswer(guesses[0])} />
            </div>
          )}

          {feedback && (
            <div className={`card cloze-feedback ${good ? 'ok' : grade === 'WRONG' ? 'bad' : ''}`}>
              <strong>
                {feedback.exact === Verdict.CORRECT
                  ? 'Correct!'
                  : feedback.exact === Verdict.ACCENTS
                    ? 'Correct, but watch the accents'
                    : 'Compare with the model answer'}
              </strong>
              {feedback.exact !== Verdict.CORRECT && (
                <>
                  <p className="diff">
                    {feedback.diff.map((w, i) => (
                      <span key={i} className={`diff-${w.kind}`}>
                        {w.word}{' '}
                      </span>
                    ))}
                  </p>
                  <span className="muted small">
                    Underlined: missing or different · struck through: not in the model · orange: accents
                  </span>
                </>
              )}
              <div>Model answer: {feedback.model}</div>
              {current.explanation && <div className="cloze-explanation">{current.explanation}</div>}
              <div>
                <ListenButton speech={speech} text={feedback.model} />
              </div>
            </div>
          )}

          <div className="flashcard-actions">
            {!feedback ? (
              <button className="primary" type="submit" disabled={!answer.trim()}>
                Check (Enter)
              </button>
            ) : !grade ? (
              <>
                <button type="button" className="danger" onClick={() => chooseGrade('WRONG')}>
                  Wrong
                </button>
                <button type="button" onClick={() => chooseGrade('NEARLY')}>
                  Nearly
                </button>
                <button type="button" className="success" onClick={() => chooseGrade('RIGHT')}>
                  Right
                </button>
              </>
            ) : (
              <button className="primary" type="submit">
                Next (Enter)
              </button>
            )}
          </div>
          {feedback && !grade && (
            <p className="muted small center">Right: a valid way to say it · Nearly: a small slip · Wrong: comes back at the end</p>
          )}
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
