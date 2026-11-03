import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading } from '../components/Status.jsx';
import Markdown from '../components/Markdown.jsx';
import LessonEditor from '../components/LessonEditor.jsx';
import VocabularyEditor from '../components/VocabularyEditor.jsx';
import SentenceEditor from '../components/SentenceEditor.jsx';
import { HighlightedSentence } from '../components/ClozeText.jsx';

export default function SubjectPage() {
  const { id } = useParams();
  const { isAdmin } = useSettings();
  const [tab, setTab] = useState('lesson');

  const { data, error, loading, setData } = useLoad(async () => {
    const [subject, lesson, vocabulary, sentences] = await Promise.all([
      api.subjects.get(id),
      api.lessons.get(id),
      api.vocabulary.list(id),
      api.sentences.list(id),
    ]);
    return { subject, lesson, vocabulary, sentences };
  }, [id]);

  if (loading) return <Loading />;
  if (error) return <ErrorMessage error={error} />;

  const { subject, lesson, vocabulary, sentences } = data;
  const setLesson = (l) => setData((d) => ({ ...d, lesson: l }));
  const setVocabulary = (v) => setData((d) => ({ ...d, vocabulary: v }));
  const setSentences = (s) => setData((d) => ({ ...d, sentences: s }));

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
        <div className="actions">
          {sentences.length > 0 && (
            <Link className="button" to={`/subjects/${id}/cloze`}>
              Fill the gap ({sentences.length})
            </Link>
          )}
          {vocabulary.length > 0 && (
            <Link className="button primary" to={`/subjects/${id}/study`}>
              Study {vocabulary.length} flashcards
            </Link>
          )}
          {vocabulary.length === 0 && sentences.length === 0 && (
            <button className="primary" disabled>
              Nothing to practise yet
            </button>
          )}
        </div>
      </div>

      <div className="tabs">
        <button className={tab === 'lesson' ? 'active' : ''} onClick={() => setTab('lesson')}>
          Lesson
        </button>
        <button className={tab === 'vocabulary' ? 'active' : ''} onClick={() => setTab('vocabulary')}>
          Vocabulary ({vocabulary.length})
        </button>
        <button className={tab === 'sentences' ? 'active' : ''} onClick={() => setTab('sentences')}>
          Sentences ({sentences.length})
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

      {tab === 'sentences' &&
        (isAdmin ? (
          <SentenceEditor subjectId={id} items={sentences} onChange={setSentences} />
        ) : sentences.length === 0 ? (
          <div className="empty">There are no sentences for this subject yet.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Sentence</th>
                <th>Translation</th>
                <th>Explanation</th>
              </tr>
            </thead>
            <tbody>
              {sentences.map((s) => (
                <tr key={s.id}>
                  <td>
                    <HighlightedSentence text={s.text} />
                  </td>
                  <td>{s.translation}</td>
                  <td className="muted">{s.explanation}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ))}
    </section>
  );
}
