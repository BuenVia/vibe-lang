import { useEffect, useState } from 'react';
import { api } from '../api.js';
import Markdown from './Markdown.jsx';
import { ErrorMessage } from './Status.jsx';

export default function LessonEditor({ subjectId, lesson, onChange }) {
  const [content, setContent] = useState(lesson?.content ?? '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [savedAt, setSavedAt] = useState(null);

  useEffect(() => setContent(lesson?.content ?? ''), [lesson]);

  const dirty = content !== (lesson?.content ?? '');

  async function save() {
    setSaving(true);
    setError(null);
    try {
      const saved = await api.lessons.save(subjectId, content);
      onChange(saved);
      setSavedAt(new Date());
    } catch (e) {
      setError(e);
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!window.confirm('Delete this lesson?')) return;
    setError(null);
    try {
      await api.lessons.remove(subjectId);
      onChange(null);
    } catch (e) {
      setError(e);
    }
  }

  return (
    <div>
      <div className="toolbar">
        <span className="muted small">
          {!lesson
            ? 'No lesson yet: write one in Markdown and save.'
            : dirty
              ? 'Unsaved changes'
              : savedAt
                ? `Saved at ${savedAt.toLocaleTimeString()}`
                : 'Saved'}
        </span>
        <div className="actions">
          {lesson && (
            <button className="danger" onClick={remove}>
              Delete lesson
            </button>
          )}
          <button className="primary" onClick={save} disabled={saving || (!dirty && !!lesson)}>
            {saving ? 'Saving…' : lesson ? 'Save' : 'Create lesson'}
          </button>
        </div>
      </div>
      <ErrorMessage error={error} />
      <div className="editor-split">
        <textarea
          className="markdown-input"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder={'# Title\n\nExplain the subject using **Markdown**…'}
          spellCheck={false}
        />
        <div className="card preview">
          {content.trim() ? <Markdown>{content}</Markdown> : <p className="muted">Preview</p>}
        </div>
      </div>
    </div>
  );
}
