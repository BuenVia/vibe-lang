// Fill the gap helpers. Sentences mark each gap with double braces: "No creo que {{tenga}} razón."
// Alternatives inside a gap are separated by "/": "Si {{tuviera/tuviese}} tiempo…".
// Mirrors android/.../ui/practice/Cloze.kt and AnswerChecker.kt so both apps grade answers the same way.

const GAP = /\{\{([^{}]*)\}\}/g;

/** Splits a sentence into [{ type: 'text', text }, { type: 'gap', index, answer }] in reading order. */
export function parseCloze(text) {
  const parts = [];
  let last = 0;
  let index = 0;
  for (const match of text.matchAll(GAP)) {
    const answer = match[1].trim();
    if (!answer) continue; // "{{}}" is not a gap; keep it as text
    if (match.index > last) parts.push({ type: 'text', text: text.slice(last, match.index) });
    parts.push({ type: 'gap', index: index++, answer });
    last = match.index + match[0].length;
  }
  if (last < text.length) parts.push({ type: 'text', text: text.slice(last) });
  return parts;
}

export const clozeGaps = (text) => parseCloze(text).filter((p) => p.type === 'gap').map((p) => p.answer);
export const hasGap = (text) => clozeGaps(text).length > 0;

/** "tuviera/tuviese" → "tuviera / tuviese" */
export const displayAnswer = (answer) =>
  answer
    .split('/')
    .map((a) => a.trim())
    .filter(Boolean)
    .join(' / ');

/**
 * Every full sentence the gaps' alternatives allow, first alternatives first ("Si {{fuera/fuese}} tú" gives
 * "Si fuera tú" and "Si fuese tú"), capped at `limit`.
 */
export function expansions(text, limit = 32) {
  let results = [''];
  for (const p of parseCloze(text)) {
    if (p.type === 'text') results = results.map((r) => r + p.text);
    else {
      const alts = p.answer.split('/').map((a) => a.trim()).filter(Boolean);
      results = results.flatMap((r) => alts.map((a) => r + a)).slice(0, limit);
    }
  }
  return results;
}

/** The sentence with each gap filled by its first alternative. */
export const solvedSentence = (text) =>
  parseCloze(text)
    .map((p) => (p.type === 'text' ? p.text : p.answer.split('/')[0].trim()))
    .join('');

// Answer checking (same rules as the Android AnswerChecker).

export const Verdict = { CORRECT: 'correct', ACCENTS: 'accents', INCORRECT: 'incorrect' };

export const normalize = (text) =>
  text
    .normalize('NFC')
    .toLowerCase()
    .replace(/['’`]/g, '')
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .trim()
    .replace(/\s+/g, ' ');

export const stripAccents = (text) => text.normalize('NFD').replace(/\p{M}+/gu, '');

function acceptedAnswers(expected) {
  const alternatives = [expected, ...expected.split(/[/;]/)];
  const forms = alternatives.flatMap((a) => [a.replace(/\(([^)]*)\)/g, '$1'), a.replace(/\(([^)]*)\)/g, ' ')]);
  return new Set(forms.map(normalize).filter(Boolean));
}

/**
 * Compares a typed answer with the expected one, ignoring case, punctuation and extra spaces, accepting
 * "/" or ";" alternatives and optional "(…)" parts. A missing accent still counts but is flagged.
 */
export function checkAnswer(answer, expected) {
  const given = normalize(answer);
  if (!given) return Verdict.INCORRECT;
  const accepted = acceptedAnswers(expected);
  if (accepted.has(given)) return Verdict.CORRECT;
  const bare = new Set([...accepted].map(stripAccents));
  return bare.has(stripAccents(given)) ? Verdict.ACCENTS : Verdict.INCORRECT;
}

/** Parses bulk lines "sentence | translation | hint | explanation" (tab also works). */
export function parseBulkSentences(text) {
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [sentence = '', translation = '', hint = '', ...rest] = line.split(/\t|\|/).map((p) => p.trim());
      return {
        text: sentence,
        translation: translation || null,
        hint: hint || null,
        explanation: rest.join(' | ') || null,
      };
    });
}

// Spoken answers (same rules as android/.../ui/practice/Spoken.kt). Each gap counts as right when one of its
// accepted answers appears as whole words after the previous gap's match; other words are ignored.

const words = (text) => normalize(text).split(' ').filter(Boolean);

/** End index of the earliest occurrence of any alternative at or after `from`, or null. */
function earliest(heard, alternatives, from) {
  let bestStart = Infinity;
  let bestEnd = null;
  for (const alt of alternatives) {
    for (let start = from; start <= heard.length - alt.length && start < bestStart; start++) {
      if (alt.every((w, i) => heard[start + i] === w)) {
        bestStart = start;
        bestEnd = start + alt.length;
        break;
      }
    }
  }
  return bestEnd;
}

/** Verdicts for one transcript. `expected` holds each gap's raw answer, e.g. "tuviera/tuviese". */
export function matchSpoken(expected, transcript) {
  const heard = words(transcript);
  const heardBare = heard.map(stripAccents);
  let from = 0;
  return expected.map((gap) => {
    const alternatives = [...acceptedAnswers(gap)].map(words).filter((a) => a.length);
    const exact = earliest(heard, alternatives, from);
    if (exact != null) {
      from = exact;
      return Verdict.CORRECT;
    }
    const bare = earliest(heardBare, alternatives.map((a) => a.map(stripAccents)), from);
    if (bare != null) {
      from = bare;
      return Verdict.ACCENTS;
    }
    return Verdict.INCORRECT;
  });
}

/** The guess with the most right gaps (exact matches breaking ties); earlier guesses win draws. */
export function bestSpoken(expected, transcripts) {
  const score = (v) => v.filter((x) => x !== Verdict.INCORRECT).length * 100 + v.filter((x) => x === Verdict.CORRECT).length;
  let best = null;
  for (const t of transcripts) {
    if (!t.trim()) continue;
    const verdicts = matchSpoken(expected, t);
    if (!best || score(verdicts) > score(best.verdicts)) best = { heard: t.trim(), verdicts };
  }
  return best;
}

/** What goes in each field after a spoken answer: the expected form for a right gap, nothing for a wrong one. */
export const spokenAnswers = (gaps, verdicts) =>
  gaps.map((g, i) => (verdicts[i] !== Verdict.INCORRECT ? g.split('/')[0].trim() : ''));
