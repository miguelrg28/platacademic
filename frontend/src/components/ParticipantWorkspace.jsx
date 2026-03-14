import { formatDateTime, formatRegistrationStatus } from '../lib/formatters'

export function ParticipantWorkspace({
  registrations,
  busyEventId,
  onCancelRegistration,
  onOpenQr,
}) {
  return (
    <section className="panel panel--wide">
      <div className="section-header">
        <div>
          <p className="eyebrow">Mi pase</p>
          <h2>Inscripciones activas y acceso QR</h2>
        </div>
        <span className="metric-chip">{registrations.length} activas</span>
      </div>

      {!registrations.length ? (
        <div className="empty-state">
          <h3>Tu panel de participante está listo</h3>
          <p>
            Cuando te inscribas a un evento, aquí aparecerá tu pase digital con el QR que usará el
            organizador para validar asistencia.
          </p>
        </div>
      ) : (
        <div className="registration-stack">
          {registrations.map((registration) => {
            const isBusy = busyEventId === registration.eventId

            return (
              <article key={registration.id} className="registration-card">
                <div className="registration-card__header">
                  <div>
                    <p className="eyebrow">Inscripción {formatRegistrationStatus(registration.status)}</p>
                    <h3>{registration.eventTitle}</h3>
                    <p>{registration.eventLocation}</p>
                  </div>
                  <span className="tag tag--muted">{formatDateTime(registration.eventStartsAt)}</span>
                </div>

                <div className="registration-card__actions">
                  <button type="button" className="secondary-button" onClick={() => onOpenQr(registration.eventId)}>
                    Ver QR
                  </button>
                  <button
                    type="button"
                    className="ghost-button"
                    disabled={isBusy}
                    onClick={() => onCancelRegistration(registration.eventId)}
                  >
                    {isBusy ? 'Cancelando...' : 'Cancelar'}
                  </button>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}
