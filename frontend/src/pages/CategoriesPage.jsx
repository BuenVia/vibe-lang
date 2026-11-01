import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { courseLabel, useSettings } from '../context/SettingsContext.jsx';
import { useLoad } from '../useLoad.js';
import { ErrorMessage, Loading, NoCourse } from '../components/Status.jsx';

export default function CategoriesPage() {
  const { course, isAdmin } = useSettings();
  const courseId = course?.id;
  const { data, error, loading } = useLoad(async () => {
    if (!courseId) return null;
    const [categories, subjects] = await Promise.all([
      api.categories.list(courseId),
      api.subjects.list(courseId),
    ]);
    return { categories, subjects };
  }, [courseId]);

  if (!course) return <NoCourse />;

  return (
    <section>
      <div className="page-header">
        <h1>Categories · {courseLabel(course)}</h1>
        {isAdmin && (
          <Link className="button" to="/admin/categories">
            Manage categories
          </Link>
        )}
      </div>
      <ErrorMessage error={error} />
      {loading || !data ? (
        <Loading />
      ) : data.categories.length === 0 ? (
        <div className="empty">No categories yet.</div>
      ) : (
        <div className="grid">
          {data.categories.map((cat) => {
            const subjects = data.subjects.filter((s) => s.categoryId === cat.id);
            return (
              <div key={cat.id} className="card">
                <h2>{cat.name}</h2>
                {cat.description && <p className="muted">{cat.description}</p>}
                {subjects.length === 0 ? (
                  <p className="muted small">No subjects.</p>
                ) : (
                  <ul className="plain-list">
                    {subjects.map((s) => (
                      <li key={s.id}>
                        <Link to={`/subjects/${s.id}`}>{s.title}</Link>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
