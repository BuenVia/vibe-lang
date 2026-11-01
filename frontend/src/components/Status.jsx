export function Loading() {
  return <p className="muted">Loading…</p>;
}

export function ErrorMessage({ error }) {
  if (!error) return null;
  return <p className="error">{error.message || String(error)}</p>;
}

export function NoCourse() {
  return (
    <div className="empty">
      <p>There are no courses yet.</p>
      <p className="muted">Switch to admin mode in Settings to create one.</p>
    </div>
  );
}
