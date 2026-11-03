import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { courseLabel, useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading, NoCourse } from '../components/Status.jsx';
import { DEFAULT_NEW_PER_DAY, currentZone } from '../reviews.jsx';

// Review counts and progress are optional: the path still shows if they can't be loaded.
const optional = (promise) => promise.catch(() => null);

export default function LearningPathPage() {
  const { course, isAdmin, coursesError, settings } = useSettings();
  const courseId = course?.id;
  const newLimit = settings.newPerDay ?? DEFAULT_NEW_PER_DAY;
  const { data, error, loading } = useLoad(async () => {
    if (!courseId) return { subjects: [], due: null, progress: {} };
    const zone = currentZone();
    const [subjects, due, progress] = await Promise.all([
      api.subjects.list(courseId),
      optional(api.reviews.due(courseId, { newLimit, zone, limit: 1 })),
      optional(api.reviews.progress(courseId, zone)),
    ]);
    return { subjects, due, progress: Object.fromEntries((progress ?? []).map((p) => [p.subjectId, p])) };
  }, [courseId, newLimit]);
  const subjects = data?.subjects ?? [];

  if (coursesError) return <ErrorMessage error={coursesError} />;
  if (!course) return <NoCourse />;

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>{courseLabel(course)}</h1>
          {course.description && <p className="muted">{course.description}</p>}
        </div>
        <div className="actions">
          <Link className="button" to="/mistakes/new">
            + Add mistake
          </Link>
          {isAdmin && (
            <Link className="button" to="/admin/subjects">
              Edit learning order
            </Link>
          )}
        </div>
      </div>

      <ErrorMessage error={error} />
      {data?.due && <ReviewCard due={data.due} />}
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

function ReviewCard({ due }) {
  const total = due.dueCount + due.newCount;
  return (
    <div className={`card review-card ${total > 0 ? 'has-work' : ''}`}>
      <div>
        <h2>Today's review</h2>
        <span className="muted">
          {total > 0 ? `${due.dueCount} due · ${due.newCount} new` : 'All caught up. Come back tomorrow.'}
        </span>
      </div>
      {total > 0 && (
        <Link className="button primary" to="/review">
          Start review
        </Link>
      )}
    </div>
  );
}

/** "3 of 12 learned" with a bar; hidden until something in the subject has been reviewed. */
function SubjectProgress({ progress }) {
  if (!progress || progress.total === 0 || progress.boxes[0] === progress.total) return null;
  const mastered = progress.boxes[4] + progress.boxes[5];
  return (
    <span className="subject-progress">
      <span className="progress">
        <span className="progress-bar" style={{ width: `${(mastered / progress.total) * 100}%` }} />
      </span>
      <span className="muted small">
        {mastered} of {progress.total} learned{progress.due > 0 ? ` · ${progress.due} due` : ''}
      </span>
    </span>
  );
}
