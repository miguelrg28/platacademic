export function ToastTray({ notices, onDismiss }) {
  if (!notices.length) {
    return null
  }

  return (
    <div className="toast-tray" aria-live="polite">
      {notices.map((notice) => (
        <article key={notice.id} className={`toast toast--${notice.tone}`}>
          <div>
            <strong>{notice.title}</strong>
            <p>{notice.message}</p>
          </div>
          <button type="button" className="ghost-button" onClick={() => onDismiss(notice.id)}>
            Cerrar
          </button>
        </article>
      ))}
    </div>
  )
}
