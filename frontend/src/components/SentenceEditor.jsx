import { useState } from 'react';
import { api } from '../api.js';
import { hasGap, parseBulkSentences } from '../cloze.js';
import { HighlightedSentence } from './ClozeText.jsx';
import { ErrorMessage } from './Status.jsx';

const EMPTY = { text: '', translation: '', hint: '', explanation: '' };
const toDraft = (s) => ({
  text: s.text,
  translation: s.translation ?? '',
  hint: s.hint ?? '',
  explanation: s.explanation ?? '',
});

/** Admin editor for a subject's Fill the gap sentences. */
export default function SentenceEditor({ subjectId, items, onChange }) {
  const [draft, setDraft] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const [editDraft, setEditDraft] = useState(EMPTY);
  const [bulkOpen, setBulkOpen] = useState(false);
  const [bulkText, setBulkText] = useState('');
  const [error, setError] = useState(null);

  async function run(action) {
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(e);
    }
  }

  const add = (e) => {
    e.preventDefault();
    run(async () => {
      const created = await api.sentences.create(subjectId, draft);
      onChange([...items, created]);
      setDraft(EMPTY);
    });
  };

  const saveEdit = () =>
    run(async () => {
      const updated = await api.sentences.update(editingId, editDraft);
      onChange(items.map((s) => (s.id === updated.id ? updated : s)));
      setEditingId(null);
    });

  const remove = (s) => {
    if (!window.confirm('Delete this sentence?')) return;
    run(async () => {
      await api.sentences.remove(s.id);
      onChange(items.filter((x) => x.id !== s.id));
    });
  };

  const parsed = parseBulkSentences(bulkText);
  const bulkInvalid = parsed.some((p) => !hasGap(p.text));

  const importBulk = () =>
    run(async () => {
      const created = await api.sentences.createMany(subjectId, parsed);
      onChange([...items, ...created]);
      setBulkText('');
      setBulkOpen(false);
    });

  const fields = (value, set, form) =>
    ['text', 'translation', 'hint', 'explanation'].map((key) => (
      <td key={key}>
        <input
          form={form}
          placeholder={PLACEHOLDERS[key]}
          value={value[key]}
          onChange={(e) => set({ ...value, [key]: e.target.value })}
        />
      </td>
    ));

  return (
    <div>
      <ErrorMessage error={error} />
      <p className="muted small">
        Put the word(s) to fill in between double braces, e.g. <code>No creo que {'{{tenga}}'} razón.</code> Use{' '}
        <code>/</code> for alternatives: <code>{'{{tuviera/tuviese}}'}</code>.
      </p>
      <table className="table">
        <thead>
          <tr>
            <th>Sentence</th>
            <th>Translation</th>
            <th>Hint</th>
            <th>Explanation</th>
            <th className="actions-col" />
          </tr>
        </thead>
        <tbody>
          {items.map((s) =>
            editingId === s.id ? (
              <tr key={s.id}>
                {fields(editDraft, setEditDraft)}
                <td className="actions">
                  <button className="primary" onClick={saveEdit} disabled={!hasGap(editDraft.text)}>
                    Save
                  </button>
                  <button onClick={() => setEditingId(null)}>Cancel</button>
                </td>
              </tr>
            ) : (
              <tr key={s.id}>
                <td>
                  <HighlightedSentence text={s.text} />
                </td>
                <td>{s.translation}</td>
                <td className="muted">{s.hint}</td>
                <td className="muted">{s.explanation}</td>
                <td className="actions">
                  <button
                    onClick={() => {
                      setEditingId(s.id);
                      setEditDraft(toDraft(s));
                    }}
                  >
                    Edit
                  </button>
                  <button className="danger" onClick={() => remove(s)}>
                    Delete
                  </button>
                </td>
              </tr>
            ),
          )}
        </tbody>
        <tfoot>
          <tr>
            {fields(draft, setDraft, 'sentence-add')}
            <td className="actions">
              <form id="sentence-add" onSubmit={add}>
                <button className="primary" disabled={!hasGap(draft.text)}>
                  Add
                </button>
              </form>
            </td>
          </tr>
        </tfoot>
      </table>

      <div className="bulk">
        <button onClick={() => setBulkOpen((o) => !o)}>{bulkOpen ? 'Hide bulk import' : 'Bulk import…'}</button>
        {bulkOpen && (
          <div className="card">
            <p className="muted small">
              One sentence per line: <code>sentence | translation | hint | explanation</code> (a tab works as a
              separator too). Only the sentence is required, and it needs at least one <code>{'{{gap}}'}</code>.
            </p>
            <textarea
              rows={8}
              value={bulkText}
              onChange={(e) => setBulkText(e.target.value)}
              placeholder={
                "No creo que {{tenga}} razón. | I don't think he's right. | tener | Doubt → subjunctive\n{{Aunque}} llueva, iremos. | Even if it rains, we'll go."
              }
            />
            <div className="toolbar">
              <span className={`small ${bulkInvalid ? 'error-text' : 'muted'}`}>
                {parsed.length} sentences{bulkInvalid ? ' — every line needs at least one {{gap}}' : ''}
              </span>
              <button className="primary" onClick={importBulk} disabled={parsed.length === 0 || bulkInvalid}>
                Import
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

const PLACEHOLDERS = {
  text: 'Sentence with {{gap}}',
  translation: 'Translation (optional)',
  hint: 'Hint, e.g. tener',
  explanation: 'Explanation (optional)',
};
