import { useState } from 'react';
import { api, move } from '../../api.js';
import { courseLabel, useSettings } from '../../context/SettingsContext.jsx';
import { useLoad } from '../../useLoad.js';
import { ErrorMessage, Loading, NoCourse } from '../../components/Status.jsx';
import { useAction } from './useAction.js';

const EMPTY = { name: '', description: '' };

export default function CategoriesAdmin() {
  const { course } = useSettings();
  const courseId = course?.id;
  const { data: categories, error: loadError, loading, setData } = useLoad(
    () => (courseId ? api.categories.list(courseId) : []),
    [courseId],
  );
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const { error, run } = useAction();

  if (!course) return <NoCourse />;
  if (loading) return <Loading />;

  const reset = () => {
    setForm(EMPTY);
    setEditingId(null);
  };

  const submit = (e) => {
    e.preventDefault();
    run(async () => {
      if (editingId) {
        const updated = await api.categories.update(editingId, form);
        setData(categories.map((c) => (c.id === updated.id ? updated : c)));
      } else {
        const created = await api.categories.create(courseId, form);
        setData([...categories, created]);
      }
      reset();
    });
  };

  const edit = (c) => {
    setEditingId(c.id);
    setForm({ name: c.name, description: c.description ?? '' });
  };

  const remove = (c) => {
    if (!window.confirm(`Delete "${c.name}" and all its subjects, lessons and vocabulary?`)) return;
    run(async () => {
      await api.categories.remove(c.id);
      setData(categories.filter((x) => x.id !== c.id));
      if (editingId === c.id) reset();
    });
  };

  const reorder = (index, delta) =>
    run(async () => {
      const next = move(categories, index, delta);
      setData(next);
      setData(await api.categories.reorder(courseId, next.map((c) => c.id)));
    });

  return (
    <div>
      <ErrorMessage error={loadError || error} />
      <form className="card form" onSubmit={submit}>
        <h2>
          {editingId ? 'Edit category' : 'New category'} <span className="muted">· {courseLabel(course)}</span>
        </h2>
        <label className="field">
          <span>Name</span>
          <input
            required
            placeholder="e.g. Verbs: Past"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </label>
        <label className="field">
          <span>Description</span>
          <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </label>
        <div className="actions">
          {editingId && (
            <button type="button" onClick={reset}>
              Cancel
            </button>
          )}
          <button className="primary">{editingId ? 'Save' : 'Create category'}</button>
        </div>
      </form>

      {categories.length === 0 ? (
        <div className="empty">No categories in this course yet.</div>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th className="order-col">Order</th>
              <th>Name</th>
              <th>Description</th>
              <th className="actions-col" />
            </tr>
          </thead>
          <tbody>
            {categories.map((c, i) => (
              <tr key={c.id}>
                <td className="order-col">
                  <button className="icon" disabled={i === 0} onClick={() => reorder(i, -1)} aria-label="Move up">
                    ↑
                  </button>
                  <button
                    className="icon"
                    disabled={i === categories.length - 1}
                    onClick={() => reorder(i, 1)}
                    aria-label="Move down"
                  >
                    ↓
                  </button>
                </td>
                <td>
                  <strong>{c.name}</strong>
                </td>
                <td className="muted">{c.description}</td>
                <td className="actions">
                  <button onClick={() => edit(c)}>Edit</button>
                  <button className="danger" onClick={() => remove(c)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
