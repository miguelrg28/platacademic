import { startTransition, useDeferredValue, useState } from 'react'

import { formatDateTime, formatEventStatus, formatRole } from '../lib/formatters'

export function AdminWorkspace({
  currentUser,
  users,
  events,
  busyUserId,
  busyEventId,
  onToggleBlocked,
  onToggleOrganizer,
  onDeleteEvent,
}) {
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)

  const filteredUsers = users.filter((user) => {
    const haystack = `${user.username} ${user.fullName} ${user.email}`.toLowerCase()
    return haystack.includes(deferredQuery.trim().toLowerCase())
  })

  const filteredEvents = events.filter((event) => {
    const haystack = `${event.title} ${event.location}`.toLowerCase()
    return haystack.includes(deferredQuery.trim().toLowerCase())
  })

  return (
    <section className="workspace-stack">
      <div className="section-header">
        <div>
          <p className="eyebrow">Administración</p>
          <h2>Panel global de moderación</h2>
        </div>
        <label className="field field--search">
          <span>Buscar</span>
          <input
            value={query}
            onChange={(event) => startTransition(() => setQuery(event.target.value))}
            placeholder="Usuario, correo o evento"
          />
        </label>
      </div>

      <div className="workspace-grid">
        <article className="panel panel--wide">
          <div className="section-header section-header--compact">
            <div>
              <p className="eyebrow">Usuarios</p>
              <h3>Control de acceso y roles</h3>
            </div>
            <span className="metric-chip">{filteredUsers.length} visibles</span>
          </div>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Usuario</th>
                  <th>Roles</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td>
                      <strong>{user.fullName}</strong>
                      <br />
                      <small>{user.username} · {user.email}</small>
                    </td>
                    <td>{user.roles.map(formatRole).join(', ')}</td>
                    <td>{user.active ? 'Activo' : 'Bloqueado'}</td>
                    <td>
                      <div className="inline-actions inline-actions--wrap">
                        <button
                          type="button"
                          className="secondary-button"
                          disabled={busyUserId === user.id || user.id === currentUser.id}
                          onClick={() => onToggleBlocked(user.id, user.active)}
                        >
                          {user.active ? 'Bloquear' : 'Desbloquear'}
                        </button>
                        <button
                          type="button"
                          className="ghost-button"
                          disabled={busyUserId === user.id || user.immutableAdmin}
                          onClick={() => onToggleOrganizer(user.id, !user.roles.includes('ORGANIZER'))}
                        >
                          {user.roles.includes('ORGANIZER') ? 'Revocar organizador' : 'Dar organizador'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>

        <article className="panel panel--wide">
          <div className="section-header section-header--compact">
            <div>
              <p className="eyebrow">Moderación</p>
              <h3>Eventos visibles para la administración</h3>
            </div>
            <span className="metric-chip">{filteredEvents.length} visibles</span>
          </div>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Evento</th>
                  <th>Fecha</th>
                  <th>Estado</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {filteredEvents.map((event) => (
                  <tr key={event.id}>
                    <td>
                      <strong>{event.title}</strong>
                      <br />
                      <small>{event.location}</small>
                    </td>
                    <td>{formatDateTime(event.startsAt)}</td>
                    <td>{formatEventStatus(event.status)}</td>
                    <td>
                      <button
                        type="button"
                        className="ghost-button"
                        disabled={busyEventId === event.id}
                        onClick={() => onDeleteEvent(event.id)}
                      >
                        {busyEventId === event.id ? 'Eliminando...' : 'Eliminar'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>
      </div>
    </section>
  )
}
