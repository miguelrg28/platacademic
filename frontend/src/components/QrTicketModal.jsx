import { useEffect } from 'react'

import { formatDateTime } from '../lib/formatters'

export function QrTicketModal({ preview, onClose }) {
  useEffect(() => {
    if (!preview.isOpen) {
      return undefined
    }

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', handleKeyDown)

    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [onClose, preview.isOpen])

  if (!preview.isOpen) {
    return null
  }

  const registration = preview.registration

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <section
        aria-labelledby="qr-ticket-title"
        aria-modal="true"
        className="modal-card qr-ticket-modal"
        role="dialog"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="qr-ticket-modal__header">
          <div>
            <p className="eyebrow">Acceso del participante</p>
            <h2 id="qr-ticket-title">
              {registration?.eventTitle ?? 'Ticket QR del evento'}
            </h2>
          </div>
          <button
            aria-label="Cerrar ticket QR"
            className="ghost-button"
            type="button"
            onClick={onClose}
          >
            Cerrar
          </button>
        </div>

        {preview.isLoading ? (
          <div className="empty-state empty-state--compact">
            <h3>Generando tu pase digital</h3>
            <p>
              Estamos consultando el backend para traer el QR oficial de esta
              inscripción.
            </p>
          </div>
        ) : preview.errorMessage ? (
          <div className="empty-state empty-state--compact">
            <h3>No fue posible mostrar el QR</h3>
            <p>{preview.errorMessage}</p>
          </div>
        ) : registration ? (
          <div className="qr-ticket-modal__content">
            <div className="qr-ticket-modal__image">
              <img
                alt={`QR para ${registration.eventTitle}`}
                src={preview.imageUrl}
              />
            </div>

            <div className="qr-ticket-modal__details">
              <div className="qr-ticket-modal__meta">
                <span className="tag tag--muted">
                  {formatDateTime(registration.eventStartsAt)}
                </span>
                <span className="tag tag--muted">{registration.eventLocation}</span>
                {registration.attendanceMarkedAt ? (
                  <span className="tag tag--success">Asistencia registrada</span>
                ) : (
                  <span className="tag tag--warm">Pendiente de escaneo</span>
                )}
              </div>

              <p className="panel-copy">
                Este QR lo genera el backend y codifica el evento, el usuario y
                el token de validación.
              </p>

              <div className="empty-state empty-state--compact">
                <h3>Preséntalo el día del evento</h3>
                <p>
                  El organizador podrá escanear este ticket para registrar tu
                  asistencia una sola vez.
                </p>
              </div>
            </div>
          </div>
        ) : null}
      </section>
    </div>
  )
}
