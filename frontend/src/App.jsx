import { Navigate, NavLink, Route, Routes, useParams } from 'react-router-dom';
import { courseLabel, useSettings } from './context/SettingsContext.jsx';
import LearningPathPage from './pages/LearningPathPage.jsx';
import CategoriesPage from './pages/CategoriesPage.jsx';
import SubjectPage from './pages/SubjectPage.jsx';
import StudyPage from './pages/StudyPage.jsx';
import ClozePage from './pages/ClozePage.jsx';
import ReviewPage from './pages/ReviewPage.jsx';
import TranslatePage from './pages/TranslatePage.jsx';
import MistakesPage from './pages/MistakesPage.jsx';
import AddMistakePage from './pages/AddMistakePage.jsx';
import SettingsPage from './pages/SettingsPage.jsx';
import AdminPage from './pages/admin/AdminPage.jsx';

export default function App() {
  const { isAdmin, courses, settings, update } = useSettings();

  return (
    <div className="app">
      <header className="topbar">
        <NavLink to="/" className="brand">
          Vibe Lang
        </NavLink>
        <nav>
          <NavLink to="/" end>
            Learning path
          </NavLink>
          <NavLink to="/categories">Categories</NavLink>
          <NavLink to="/mistakes">Mistakes</NavLink>
          {isAdmin && <NavLink to="/admin">Admin</NavLink>}
          <NavLink to="/settings">Settings</NavLink>
        </nav>
        <div className="topbar-right">
          {courses.length > 0 && (
            <select
              aria-label="Course"
              value={settings.courseId ?? ''}
              onChange={(e) => update({ courseId: Number(e.target.value) })}
            >
              {courses.map((c) => (
                <option key={c.id} value={c.id}>
                  {courseLabel(c)}
                </option>
              ))}
            </select>
          )}
          <span className={`mode-badge ${isAdmin ? 'admin' : 'learner'}`}>
            {isAdmin ? 'Admin mode' : 'Learner mode'}
          </span>
        </div>
      </header>

      <main className="content">
        <Routes>
          <Route path="/" element={<LearningPathPage />} />
          <Route path="/categories" element={<CategoriesPage />} />
          <Route path="/review" element={<ReviewPage />} />
          <Route path="/mistakes" element={<MistakesPage />} />
          <Route path="/mistakes/new" element={<AddMistakePage />} />
          <Route path="/mistakes/practice" element={<ClozePage mistakes />} />
          <Route path="/subjects/:id" element={<SubjectPage />} />
          <Route path="/subjects/:id/study" element={<StudyPage />} />
          <Route path="/subjects/:id/cloze" element={<ClozePage />} />
          <Route path="/subjects/:id/translate" element={<TranslatePage />} />
          <Route path="/subjects/:id/flashcards" element={<OldFlashcardsRedirect />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/admin/*" element={isAdmin ? <AdminPage /> : <Navigate to="/settings" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

/** Keeps bookmarks to the old flashcards URL working after it was renamed to Study. */
function OldFlashcardsRedirect() {
  const { id } = useParams();
  return <Navigate to={`/subjects/${id}/study`} replace />;
}
