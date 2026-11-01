import { Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { courseLabel, useSettings } from './context/SettingsContext.jsx';
import StudyPathPage from './pages/StudyPathPage.jsx';
import CategoriesPage from './pages/CategoriesPage.jsx';
import SubjectPage from './pages/SubjectPage.jsx';
import FlashcardsPage from './pages/FlashcardsPage.jsx';
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
          <span className={`mode-badge ${isAdmin ? 'admin' : 'study'}`}>
            {isAdmin ? 'Admin mode' : 'Study mode'}
          </span>
        </div>
      </header>

      <main className="content">
        <Routes>
          <Route path="/" element={<StudyPathPage />} />
          <Route path="/categories" element={<CategoriesPage />} />
          <Route path="/subjects/:id" element={<SubjectPage />} />
          <Route path="/subjects/:id/flashcards" element={<FlashcardsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/admin/*" element={isAdmin ? <AdminPage /> : <Navigate to="/settings" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
