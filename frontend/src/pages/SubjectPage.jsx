import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading } from '../components/Status.jsx';
import Markdown from '../components/Markdown.jsx';
import LessonEditor from '../components/LessonEditor.jsx';
import VocabularyEditor from '../components/VocabularyEditor.jsx';

export default function SubjectPage() {
  const { id } = useParams();
  const { isAdmin } = useSettings();
  const [tab, setTab] = useState('lesson');

  const { data, error, loading, setData } = useLoad(async () => {
    const [subject, lesson, vocabulary] = await Promise.all([
      api.subjects.get(id),
      api.lessons.get(id),
      api.vocabulary.list(id),
    ]);
    return { subject, lesson, vocabulary };
  }, [id]);

  if (loading) return <Loading />;
  if (error) return <ErrorMessage error={error} />;

  const { subject, lesson, vocabulary } = data;
  const setLesson = (l) => setData((d) => ({ ...d, lesson: l }));
  const setVocabulary = (v) => setData((d) => ({ ...d, vocabulary: v }));

  return (
    <section>
      <p className="breadcrumb">
        <Link to="/">Learning path</Link> / <span>{subject.categoryName}</span>
      </p>
      <div className="page-header">
        <div>
          <h1>{subject.title}</h1>
          {subject.description && <p className="muted">{subject.description}</p>}
        </div>
        {vocabulary.length > 0 ? (
          <Link className="button primary" to={`/subjects/${id}/flashcards`}>
            Practise {vocabulary.length} flashcards
          </Link>
        ) : (
          <button className="primary" disabled>
            No flashcards yet
          </button>
        )}
      </div>

      <div className="tabs">
        <button className={tab === 'lesson' ? 'active' : ''} onClick={() => setTab('lesson')}>
          Lesson
        </button>
        <button className={tab === 'vocabulary' ? 'active' : ''} onClick={() => setTab('vocabulary')}>
          Vocabulary ({vocabulary.length})
        </button>
      </div>

      {tab === 'lesson' &&
        (isAdmin ? (
          <LessonEditor subjectId={id} lesson={lesson} onChange={setLesson} />
        ) : lesson ? (
          <article className="card">
            <Markdown>{lesson.content}</Markdown>
          </article>
        ) : (
          <div className="empty">There is no lesson for this subject yet.</div>
        ))}

      {tab === 'vocabulary' &&
        (isAdmin ? (
          <VocabularyEditor subjectId={id} items={vocabulary} onChange={setVocabulary} />
        ) : vocabulary.length === 0 ? (
          <div className="empty">There is no vocabulary for this subject yet.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Term</th>
                <th>Translation</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              {vocabulary.map((v) => (
                <tr key={v.id}>
                  <td>
                    <strong>{v.term}</strong>
                  </td>
                  <td>{v.translation}</td>
                  <td className="muted">{v.notes}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ))}
    </section>
  );
}
