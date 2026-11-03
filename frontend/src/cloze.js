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

/** The sentence with each gap filled by its first alternative. */
export const solvedSentence = (text) =>
  parseCloze(text)
    .map((p) => (p.type === 'text' ? p.text : p.answer.split('/')[0].trim()))
    .join('');

// Answer checking (same rules as the Android AnswerChecker).

export const Verdict = { CORRECT: 'correct', ACCENTS: 'accents', INCORRECT: 'incorrect' };

const normalize = (text) =>
  text
    .normalize('NFC')
    .toLowerCase()
    .replace(/['’`]/g, '')
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .trim()
    .replace(/\s+/g, ' ');

const stripAccents = (text) => text.normalize('NFD').replace(/\p{M}+/gu, '');

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
