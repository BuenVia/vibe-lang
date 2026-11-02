import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';

const STORAGE_KEY = 'vibe-lang.settings';
const DEFAULTS = {
  mode: 'learner', // 'learner' | 'admin'
  courseId: null,
  cardFront: 'term', // 'term' | 'translation'
  shuffleCards: true,
};

function loadSettings() {
  try {
    const saved = { ...DEFAULTS, ...JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') };
    // 'study' was the old name of learner mode; anything that isn't admin is learner.
    return { ...saved, mode: saved.mode === 'admin' ? 'admin' : 'learner' };
  } catch {
    return DEFAULTS;
  }
}

const SettingsContext = createContext(null);

export function SettingsProvider({ children }) {
  const [settings, setSettings] = useState(loadSettings);
  const [courses, setCourses] = useState([]);
  const [coursesError, setCoursesError] = useState(null);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    } catch {
      // storage unavailable: settings just won't persist
    }
  }, [settings]);

  const update = useCallback((patch) => setSettings((s) => ({ ...s, ...patch })), []);

  const reloadCourses = useCallback(() => {
    return api.courses
      .list()
      .then((list) => {
        setCourses(list);
        setCoursesError(null);
        // Keep the selected course valid.
        setSettings((s) =>
          list.some((c) => c.id === s.courseId) ? s : { ...s, courseId: list[0]?.id ?? null },
        );
      })
      .catch(setCoursesError);
  }, []);

  useEffect(() => {
    reloadCourses();
  }, [reloadCourses]);

  const value = useMemo(
    () => ({
      settings,
      update,
      isAdmin: settings.mode === 'admin',
      courses,
      coursesError,
      reloadCourses,
      course: courses.find((c) => c.id === settings.courseId) ?? null,
    }),
    [settings, update, courses, coursesError, reloadCourses],
  );

  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>;
}

export function useSettings() {
  return useContext(SettingsContext);
}

export function courseLabel(course) {
  return course ? `${course.language} ${course.level}` : '';
}
