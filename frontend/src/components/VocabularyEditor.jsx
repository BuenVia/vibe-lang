import { useState } from 'react';
import { api } from '../api.js';
import { ErrorMessage } from './Status.jsx';

const EMPTY = { term: '', translation: '', notes: '' };

/** Parses lines like "term | translation | notes" (tab also works as a separator). */
function parseBulk(text) {
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [term = '', translation = '', ...rest] = line.split(/\t|\|/).map((p) => p.trim());
      return { term, translation, notes: rest.join(' | ') || null };
    });
}

export default function VocabularyEditor({ subjectId, items, onChange }) {
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
      const created = await api.vocabulary.create(subjectId, draft);
      onChange([...items, created]);
      setDraft(EMPTY);
    });
  };

  const startEdit = (v) => {
    setEditingId(v.id);
    setEditDraft({ term: v.term, translation: v.translation, notes: v.notes ?? '' });
  };

  const saveEdit = () =>
    run(async () => {
      const updated = await api.vocabulary.update(editingId, editDraft);
      onChange(items.map((v) => (v.id === updated.id ? updated : v)));
      setEditingId(null);
    });

  const remove = (v) => {
    if (!window.confirm(`Delete "${v.term}"?`)) return;
    run(async () => {
      await api.vocabulary.remove(v.id);
      onChange(items.filter((x) => x.id !== v.id));
    });
  };

  const parsed = parseBulk(bulkText);
  const bulkInvalid = parsed.some((p) => !p.term || !p.translation);

  const importBulk = () =>
    run(async () => {
      const created = await api.vocabulary.createMany(subjectId, parsed);
      onChange([...items, ...created]);
      setBulkText('');
      setBulkOpen(false);
    });

  return (
    <div>
      <ErrorMessage error={error} />
      <table className="table">
        <thead>
          <tr>
            <th>Term</th>
            <th>Translation</th>
            <th>Notes</th>
            <th className="actions-col" />
          </tr>
        </thead>
        <tbody>
          {items.map((v) =>
            editingId === v.id ? (
              <tr key={v.id}>
                <td>
                  <input
                    value={editDraft.term}
                    onChange={(e) => setEditDraft({ ...editDraft, term: e.target.value })}
                  />
                </td>
                <td>
                  <input
                    value={editDraft.translation}
                    onChange={(e) => setEditDraft({ ...editDraft, translation: e.target.value })}
                  />
                </td>
                <td>
                  <input
                    value={editDraft.notes}
                    onChange={(e) => setEditDraft({ ...editDraft, notes: e.target.value })}
                  />
                </td>
                <td className="actions">
                  <button className="primary" onClick={saveEdit}>
                    Save
                  </button>
                  <button onClick={() => setEditingId(null)}>Cancel</button>
                </td>
              </tr>
            ) : (
              <tr key={v.id}>
                <td>
                  <strong>{v.term}</strong>
                </td>
                <td>{v.translation}</td>
                <td className="muted">{v.notes}</td>
                <td className="actions">
                  <button onClick={() => startEdit(v)}>Edit</button>
                  <button className="danger" onClick={() => remove(v)}>
                    Delete
                  </button>
                </td>
              </tr>
            ),
          )}
        </tbody>
        <tfoot>
          <tr>
            <td>
              <input
                form="vocab-add"
                placeholder="Term"
                value={draft.term}
                onChange={(e) => setDraft({ ...draft, term: e.target.value })}
              />
            </td>
            <td>
              <input
                form="vocab-add"
                placeholder="Translation"
                value={draft.translation}
                onChange={(e) => setDraft({ ...draft, translation: e.target.value })}
              />
            </td>
            <td>
              <input
                form="vocab-add"
                placeholder="Notes / example (optional)"
                value={draft.notes}
                onChange={(e) => setDraft({ ...draft, notes: e.target.value })}
              />
            </td>
            <td className="actions">
              <form id="vocab-add" onSubmit={add}>
                <button className="primary" disabled={!draft.term.trim() || !draft.translation.trim()}>
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
              One entry per line: <code>term | translation | notes</code> (a tab works as a separator too; notes
              are optional).
            </p>
            <textarea
              rows={8}
              value={bulkText}
              onChange={(e) => setBulkText(e.target.value)}
              placeholder={'a menudo | often\nmientras | while | Mientras yo cocinaba, ella leía.'}
            />
            <div className="toolbar">
              <span className={`small ${bulkInvalid ? 'error-text' : 'muted'}`}>
                {parsed.length} entries{bulkInvalid ? ' — every line needs a term and a translation' : ''}
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
