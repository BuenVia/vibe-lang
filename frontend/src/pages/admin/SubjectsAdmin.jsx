import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, move } from '../../api.js';
import { courseLabel, useSettings } from '../../context/SettingsContext.jsx';
import { useLoad } from '../../useLoad.js';
import { ErrorMessage, Loading, NoCourse } from '../../components/Status.jsx';
import { useAction } from './useAction.js';

const EMPTY = { categoryId: '', title: '', description: '' };

export default function SubjectsAdmin() {
  const { course } = useSettings();
  const courseId = course?.id;
  const { data, error: loadError, loading, setData } = useLoad(async () => {
    if (!courseId) return null;
    const [categories, subjects] = await Promise.all([api.categories.list(courseId), api.subjects.list(courseId)]);
    return { categories, subjects };
  }, [courseId]);
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const { error, run } = useAction();

  if (!course) return <NoCourse />;
  if (loading || !data) return <Loading />;

  const { categories, subjects } = data;
  const setSubjects = (next) => setData((d) => ({ ...d, subjects: next }));

  const reset = () => {
    setForm(EMPTY);
    setEditingId(null);
  };

  const submit = (e) => {
    e.preventDefault();
    run(async () => {
      const body = { ...form, categoryId: Number(form.categoryId) };
      if (editingId) {
        await api.subjects.update(editingId, body);
      } else {
        await api.subjects.create(body);
      }
      setSubjects(await api.subjects.list(courseId));
      reset();
    });
  };

  const edit = (s) => {
    setEditingId(s.id);
    setForm({ categoryId: String(s.categoryId), title: s.title, description: s.description ?? '' });
  };

  const remove = (s) => {
    if (!window.confirm(`Delete "${s.title}" with its lesson and vocabulary?`)) return;
    run(async () => {
      await api.subjects.remove(s.id);
      setSubjects(subjects.filter((x) => x.id !== s.id));
      if (editingId === s.id) reset();
    });
  };

  const reorder = (index, delta) =>
    run(async () => {
      const next = move(subjects, index, delta);
      setSubjects(next);
      setSubjects(await api.subjects.reorder(courseId, next.map((s) => s.id)));
    });

  return (
    <div>
      <ErrorMessage error={loadError || error} />
      {categories.length === 0 ? (
        <div className="empty">
          Create a <Link to="/admin/categories">category</Link> first — every subject belongs to one.
        </div>
      ) : (
        <form className="card form" onSubmit={submit}>
          <h2>
            {editingId ? 'Edit subject' : 'New subject'} <span className="muted">· {courseLabel(course)}</span>
          </h2>
          <div className="form-row">
            <label className="field">
              <span>Title</span>
              <input
                required
                placeholder="e.g. Pretérito imperfecto"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
            </label>
            <label className="field">
              <span>Category</span>
              <select
                required
                value={form.categoryId}
                onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
              >
                <option value="" disabled>
                  Choose…
                </option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
          </div>
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
            <button className="primary">{editingId ? 'Save' : 'Create subject'}</button>
          </div>
        </form>
      )}

      <p className="muted small">
        Subjects are listed in learning order. Use the arrows to change it. Lessons and vocabulary are edited on
        each subject's page.
      </p>

      {subjects.length === 0 ? (
        <div className="empty">No subjects in this course yet.</div>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th className="order-col">Order</th>
              <th>Subject</th>
              <th>Category</th>
              <th>Content</th>
              <th className="actions-col" />
            </tr>
          </thead>
          <tbody>
            {subjects.map((s, i) => (
              <tr key={s.id}>
                <td className="order-col">
                  <span className="order-number">{i + 1}</span>
                  <button className="icon" disabled={i === 0} onClick={() => reorder(i, -1)} aria-label="Move up">
                    ↑
                  </button>
                  <button
                    className="icon"
                    disabled={i === subjects.length - 1}
                    onClick={() => reorder(i, 1)}
                    aria-label="Move down"
                  >
                    ↓
                  </button>
                </td>
                <td>
                  <strong>{s.title}</strong>
                  {s.description && <div className="muted small">{s.description}</div>}
                </td>
                <td>
                  <span className="tag">{s.categoryName}</span>
                </td>
                <td className="muted small">
                  {s.hasLesson ? 'Lesson ✓' : 'No lesson'} · {s.vocabularyCount} words
                </td>
                <td className="actions">
                  <Link className="button" to={`/subjects/${s.id}`}>
                    Lesson &amp; vocab
                  </Link>
                  <button onClick={() => edit(s)}>Edit</button>
                  <button className="danger" onClick={() => remove(s)}>
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
