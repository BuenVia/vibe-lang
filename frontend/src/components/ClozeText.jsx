import { Verdict, displayAnswer, parseCloze } from '../cloze.js';

/** Blanks before checking; the answers coloured in (green right, red wrong) afterwards. */
export function ClozeSentence({ text, verdicts }) {
  const parts = parseCloze(text);
  const numbered = parts.filter((p) => p.type === 'gap').length > 1;
  return parts.map((p, i) => {
    if (p.type === 'text') return <span key={i}>{p.text}</span>;
    if (!verdicts) return <span key={i} className="cloze-blank">{numbered ? `____(${p.index + 1})` : '______'}</span>;
    const ok = verdicts[p.index] !== Verdict.INCORRECT;
    return (
      <span key={i} className={`cloze-answer ${ok ? 'ok' : 'bad'}`}>
        {displayAnswer(p.answer)}
      </span>
    );
  });
}

/** The sentence with each gap's answer highlighted, used in sentence lists. */
export function HighlightedSentence({ text }) {
  return parseCloze(text).map((p, i) =>
    p.type === 'text' ? (
      <span key={i}>{p.text}</span>
    ) : (
      <span key={i} className="cloze-answer">
        {displayAnswer(p.answer)}
      </span>
    ),
  );
}
