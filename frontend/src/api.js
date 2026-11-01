// Empty in development (Vite proxies /api to localhost:8080).
// In production set VITE_API_BASE_URL at build time, e.g. https://vibe-lang-api.onrender.com
const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

async function request(method, path, body) {
  const options = { method, headers: {} };
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }
  const res = await fetch(`${API_BASE}/api${path}`, options);
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const error = new Error(data?.detail || data?.title || `Request failed (${res.status})`);
    error.status = res.status;
    throw error;
  }
  return data;
}

const get = (path) => request('GET', path);
const post = (path, body) => request('POST', path, body);
const put = (path, body) => request('PUT', path, body);
const del = (path) => request('DELETE', path);

export const api = {
  courses: {
    list: () => get('/courses'),
    create: (body) => post('/courses', body),
    update: (id, body) => put(`/courses/${id}`, body),
    remove: (id) => del(`/courses/${id}`),
  },
  categories: {
    list: (courseId) => get(`/courses/${courseId}/categories`),
    create: (courseId, body) => post(`/courses/${courseId}/categories`, body),
    reorder: (courseId, ids) => put(`/courses/${courseId}/categories/order`, { ids }),
    update: (id, body) => put(`/categories/${id}`, body),
    remove: (id) => del(`/categories/${id}`),
  },
  subjects: {
    list: (courseId) => get(`/courses/${courseId}/subjects`),
    reorder: (courseId, ids) => put(`/courses/${courseId}/subjects/order`, { ids }),
    get: (id) => get(`/subjects/${id}`),
    create: (body) => post('/subjects', body),
    update: (id, body) => put(`/subjects/${id}`, body),
    remove: (id) => del(`/subjects/${id}`),
  },
  lessons: {
    // Resolves to null when the subject has no lesson yet.
    get: (subjectId) =>
      get(`/subjects/${subjectId}/lesson`).catch((e) => {
        if (e.status === 404) return null;
        throw e;
      }),
    save: (subjectId, content) => put(`/subjects/${subjectId}/lesson`, { content }),
    remove: (subjectId) => del(`/subjects/${subjectId}/lesson`),
  },
  vocabulary: {
    list: (subjectId) => get(`/subjects/${subjectId}/vocabulary`),
    create: (subjectId, body) => post(`/subjects/${subjectId}/vocabulary`, body),
    createMany: (subjectId, items) => post(`/subjects/${subjectId}/vocabulary/bulk`, { items }),
    update: (id, body) => put(`/vocabulary/${id}`, body),
    remove: (id) => del(`/vocabulary/${id}`),
  },
};

/** Returns a copy of `list` with the item at `index` moved by `delta` (-1 up, +1 down). */
export function move(list, index, delta) {
  const target = index + delta;
  if (target < 0 || target >= list.length) return list;
  const copy = [...list];
  [copy[index], copy[target]] = [copy[target], copy[index]];
  return copy;
}
