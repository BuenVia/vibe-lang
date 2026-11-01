import { useState } from 'react';
import { api } from '../../api.js';
import { courseLabel, useSettings } from '../../context/SettingsContext.jsx';
import { ErrorMessage } from '../../components/Status.jsx';
import { useAction } from './useAction.js';

const EMPTY = { language: '', level: '', description: '' };

export default function CoursesAdmin() {
  const { courses, reloadCourses, update } = useSettings();
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const { error, run } = useAction();

  const reset = () => {
    setForm(EMPTY);
    setEditingId(null);
  };

  const submit = (e) => {
    e.preventDefault();
    run(async () => {
      if (editingId) {
        await api.courses.update(editingId, form);
      } else {
        const created = await api.courses.create(form);
        update({ courseId: created.id });
      }
      await reloadCourses();
      reset();
    });
  };

  const edit = (c) => {
    setEditingId(c.id);
    setForm({ language: c.language, level: c.level, description: c.description ?? '' });
  };

  const remove = (c) => {
    if (!window.confirm(`Delete ${courseLabel(c)} and ALL its categories, subjects, lessons and vocabulary?`)) return;
    run(async () => {
      await api.courses.remove(c.id);
      await reloadCourses();
      if (editingId === c.id) reset();
    });
  };

  return (
    <div>
      <ErrorMessage error={error} />
      <form className="card form" onSubmit={submit}>
        <h2>{editingId ? 'Edit course' : 'New course'}</h2>
        <div className="form-row">
          <label className="field">
            <span>Language</span>
            <input
              required
              placeholder="e.g. Italian"
              value={form.language}
              onChange={(e) => setForm({ ...form, language: e.target.value })}
            />
          </label>
          <label className="field small-field">
            <span>Level</span>
            <input
              required
              placeholder="e.g. A2"
              value={form.level}
              onChange={(e) => setForm({ ...form, level: e.target.value })}
            />
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
          <button className="primary">{editingId ? 'Save' : 'Create course'}</button>
        </div>
      </form>

      <table className="table">
        <thead>
          <tr>
            <th>Course</th>
            <th>Description</th>
            <th className="actions-col" />
          </tr>
        </thead>
        <tbody>
          {courses.map((c) => (
            <tr key={c.id}>
              <td>
                <strong>{courseLabel(c)}</strong>
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
    </div>
  );
}
