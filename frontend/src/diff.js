// Word-level comparison of a written sentence with a model answer, for Translate and for marking a mistake's
// differences. Mirrors android/.../ui/practice/TextDiff.kt; keep the two in step.
import { Verdict, normalize, stripAccents } from './cloze.js';

export const Kind = { SAME: 'same', ACCENT: 'accent', MISSING: 'missing', EXTRA: 'extra' };

const tokens = (text) =>
  text
    .split(/\s+/)
    .filter(Boolean)
    .map((raw) => {
      const key = normalize(raw).replace(/ /g, '');
      return key ? { text: raw, key, bare: stripAccents(key) } : null;
    })
    .filter(Boolean);

/** [{ word, kind }]: the model's words in order, with the answer's extra words where they were written. */
export function diff(answer, model) {
  const a = tokens(answer);
  const m = tokens(model);
  const lcs = Array.from({ length: a.length + 1 }, () => new Array(m.length + 1).fill(0));
  for (let i = a.length - 1; i >= 0; i--)
    for (let j = m.length - 1; j >= 0; j--)
      lcs[i][j] = a[i].bare === m[j].bare ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
  const out = [];
  let i = 0;
  let j = 0;
  while (i < a.length || j < m.length) {
    if (i < a.length && j < m.length && a[i].bare === m[j].bare) {
      out.push({ word: m[j].text, kind: a[i].key === m[j].key ? Kind.SAME : Kind.ACCENT });
      i++;
      j++;
    } else if (j < m.length && (i === a.length || lcs[i][j + 1] >= lcs[i + 1][j])) {
      out.push({ word: m[j++].text, kind: Kind.MISSING });
    } else {
      out.push({ word: a[i++].text, kind: Kind.EXTRA });
    }
  }
  return out;
}

const distance = (d) => d.reduce((n, w) => n + (w.kind === Kind.SAME ? 0 : w.kind === Kind.ACCENT ? 1 : 2), 0);

/** { model, diff } against the closest model (first wins ties). */
export function closest(answer, models) {
  let best = null;
  for (const model of models) {
    const d = diff(answer, model);
    if (!best || distance(d) < distance(best.diff)) best = { model, diff: d };
  }
  return best;
}

/** Whole-sentence check: "/" and ";" are part of the sentence here, not alternatives. */
export function checkSentence(answer, models) {
  const given = normalize(answer);
  if (!given) return Verdict.INCORRECT;
  const normalized = models.map(normalize);
  if (normalized.includes(given)) return Verdict.CORRECT;
  return normalized.map(stripAccents).includes(stripAccents(given)) ? Verdict.ACCENTS : Verdict.INCORRECT;
}

const isWordChar = (c) => /[\p{L}\p{N}]/u.test(c);

/**
 * Wraps the words of `correct` that differ from `wrong` in {{…}}, merging neighbours and keeping punctuation
 * outside: ("No creo que tiene razón.", "No creo que tenga razón.") → "No creo que {{tenga}} razón."
 * Returns `correct` unchanged when nothing differs.
 */
export function markDifferences(wrong, correct) {
  const words = correct.split(' ').filter(Boolean);
  const changed = new Array(words.length).fill(false);
  let k = 0;
  for (const w of diff(wrong, correct)) {
    if (w.kind === Kind.EXTRA) continue;
    while (k < words.length && !normalize(words[k])) k++;
    if (k >= words.length) break;
    if (w.kind === Kind.MISSING || w.kind === Kind.ACCENT) changed[k] = true;
    k++;
  }
  if (!changed.some(Boolean)) return correct;
  const out = [];
  for (let idx = 0; idx < words.length; ) {
    if (!changed[idx]) {
      out.push(words[idx++]);
      continue;
    }
    let end = idx;
    while (end + 1 < words.length && changed[end + 1]) end++;
    const run = words.slice(idx, end + 1).join(' ');
    let lead = 0;
    while (lead < run.length && !isWordChar(run[lead])) lead++;
    let trail = run.length;
    while (trail > lead && !isWordChar(run[trail - 1])) trail--;
    out.push(`${run.slice(0, lead)}{{${run.slice(lead, trail)}}}${run.slice(trail)}`);
    idx = end + 1;
  }
  return out.join(' ');
}
