import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { courseLabel, useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading, NoCourse } from '../components/Status.jsx';

export default function StudyPathPage() {
  const { course, isAdmin, coursesError } = useSettings();
  const courseId = course?.id;
  const { data: subjects, error, loading } = useLoad(
    () => (courseId ? api.subjects.list(courseId) : []),
    [courseId],
  );

  if (coursesError) return <ErrorMessage error={coursesError} />;
  if (!course) return <NoCourse />;

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>{courseLabel(course)}</h1>
          {course.description && <p className="muted">{course.description}</p>}
        </div>
        {isAdmin && (
          <Link className="button" to="/admin/subjects">
            Edit learning order
          </Link>
        )}
      </div>

      <ErrorMessage error={error} />
      {loading ? (
        <Loading />
      ) : subjects.length === 0 ? (
        <div className="empty">
          <p>No subjects in this course yet.</p>
        </div>
      ) : (
        <ol className="path">
          {subjects.map((s, i) => (
            <li key={s.id}>
              <Link to={`/subjects/${s.id}`} className="path-item">
                <span className="path-number">{i + 1}</span>
                <span className="path-body">
                  <span className="path-title">{s.title}</span>
                  {s.description && <span className="muted">{s.description}</span>}
                </span>
                <span className="path-meta">
                  <span className="tag">{s.categoryName}</span>
                  <span className="muted small">
                    {s.hasLesson ? 'Lesson' : 'No lesson'} · {s.vocabularyCount} words
                  </span>
                </span>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
