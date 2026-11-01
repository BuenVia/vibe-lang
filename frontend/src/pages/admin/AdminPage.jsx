import { Navigate, NavLink, Route, Routes } from 'react-router-dom';
import CoursesAdmin from './CoursesAdmin.jsx';
import CategoriesAdmin from './CategoriesAdmin.jsx';
import SubjectsAdmin from './SubjectsAdmin.jsx';

export default function AdminPage() {
  return (
    <section>
      <h1>Admin</h1>
      <div className="tabs">
        <NavLink to="/admin/courses">Courses</NavLink>
        <NavLink to="/admin/categories">Categories</NavLink>
        <NavLink to="/admin/subjects">Subjects</NavLink>
      </div>
      <Routes>
        <Route path="courses" element={<CoursesAdmin />} />
        <Route path="categories" element={<CategoriesAdmin />} />
        <Route path="subjects" element={<SubjectsAdmin />} />
        <Route path="*" element={<Navigate to="/admin/subjects" replace />} />
      </Routes>
    </section>
  );
}
