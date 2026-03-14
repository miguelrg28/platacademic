import { startTransition, useDeferredValue, useState } from 'react'

import { formatDateTime } from '../lib/formatters'

export function EventCatalog({
  events,
  currentUser,
  registeredEventIds,
  busyEventId,
  onRegister,
  onCancelRegistration,
  onOpenQr,
}) {
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)

  const filteredEvents = events.filter((event) => {
    const haystack = `${event.title} ${event.description} ${event.location}`.toLowerCase()
    return haystack.includes(deferredQuery.trim().toLowerCase())
  })

  return (
    <section className="panel panel--wide">
      <div className="section-header">
        <div>
          <p className="eyebrow">Cartelera</p>
          <h2>Eventos académicos publicados</h2>
        </div>
        <label className="field field--search">
          <span>Buscar</span>
          <input
            value={query}
            onChange={(event) => startTransition(() => setQuery(event.target.value))}
            placeholder="Título, lugar o palabra clave"
          />
        </label>
      </div>

      <div className="event-grid">
        {filteredEvents.map((event) => {
          const isRegistered = registeredEventIds.has(event.id)
          const isBusy = busyEventId === event.id

          return (
            <article key={event.id} className="event-card">
              <div className="event-card__meta">
                <span className="tag tag--warm">{formatDateTime(event.startsAt)}</span>
                <span className="tag tag--muted">{event.location}</span>
              </div>
              <h3>{event.title}</h3>
              <p>{event.description}</p>
              <div className="event-card__footer">
                <div>
                  <strong>{event.maxCapacity}</strong>
                  <span>Cupo máximo</span>
                </div>
                {currentUser ? (
                  isRegistered ? (
                    <div className="inline-actions">
                      <button type="button" className="secondary-button" onClick={() => onOpenQr(event.id)}>
                        Ver QR
                      </button>
                      <button
                        type="button"
                        className="ghost-button"
                        disabled={isBusy}
                        onClick={() => onCancelRegistration(event.id)}
                      >
                        {isBusy ? 'Cancelando...' : 'Cancelar inscripción'}
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      className="primary-button"
                      disabled={isBusy}
                      onClick={() => onRegister(event.id)}
                    >
                      {isBusy ? 'Inscribiendo...' : 'Inscribirme'}
                    </button>
                  )
                ) : (
                  <p className="panel-copy panel-copy--compact">Inicia sesión para reservar tu espacio.</p>
                )}
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
