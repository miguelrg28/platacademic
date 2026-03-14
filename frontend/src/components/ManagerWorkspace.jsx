import { useState } from 'react'

import { formatDateTime, formatEventStatus } from '../lib/formatters'
import { QrScannerPanel } from './QrScannerPanel'
import { SummaryCharts } from './SummaryCharts'

function createEmptyDraft() {
    return {
        title: '',
        description: '',
        startsAt: '',
        location: '',
        maxCapacity: 40,
    }
}

function draftFromEvent(event) {
    if (!event) {
        return createEmptyDraft()
    }

    return {
        title: event.title,
        description: event.description,
        startsAt: event.startsAt.slice(0, 16),
        location: event.location,
        maxCapacity: event.maxCapacity,
    }
}

export function ManagerWorkspace({
    events,
    selectedEventId,
    selectedEventRegistrations,
    selectedSummary,
    loadingInsights,
    busyEventId,
    managerSubmitting,
    onSelectEvent,
    onCreateEvent,
    onUpdateEvent,
    onPublishEvent,
    onUnpublishEvent,
    onCancelEvent,
    onScanAttendance,
    onScanAttendanceImage,
}) {
    const [mode, setMode] = useState('create')
    const [draft, setDraft] = useState(createEmptyDraft())
    const [formError, setFormError] = useState('')

    const selectedEvent = events.find((event) => event.id === selectedEventId) ?? null

    async function handleSubmit(event) {
        event.preventDefault()
        setFormError('')

        const payload = {
            ...draft,
            maxCapacity: Number(draft.maxCapacity),
        }

        try {
            if (mode === 'edit' && selectedEvent) {
                await onUpdateEvent(selectedEvent.id, payload)
            } else {
                const created = await onCreateEvent(payload)
                setMode('edit')
                setDraft(draftFromEvent(created))
            }
        } catch (error) {
            setFormError(error.message)
        }
    }

    return (
        <section className="workspace-stack">
            <div className="section-header">
                <div>
                    <p className="eyebrow">Gestión</p>
                    <h2>Centro operativo del organizador</h2>
                </div>
                <div className="inline-actions">
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={() => {
                            setMode('create')
                            setDraft(createEmptyDraft())
                            setFormError('')
                        }}
                    >
                        Nuevo evento
                    </button>
                    <span className="metric-chip">{events.length} eventos gestionables</span>
                </div>
            </div>

            <div className="workspace-grid">
                <article className="panel">
                    <div className="section-header section-header--compact">
                        <div>
                            <p className="eyebrow">Portafolio</p>
                            <h3>Eventos a tu cargo</h3>
                        </div>
                    </div>
                    <div className="selection-list">
                        {events.map((event) => (
                            <button
                                key={event.id}
                                type="button"
                                className={`selection-card ${event.id === selectedEventId ? 'selection-card--active' : ''}`}
                                onClick={() => {
                                    onSelectEvent(event.id)
                                    setMode('edit')
                                    setDraft(draftFromEvent(event))
                                }}
                            >
                                <div>
                                    <strong>{event.title}</strong>
                                    <p>{event.location}</p>
                                </div>
                                <div className="selection-card__meta">
                                    <span className="tag tag--muted">
                                        {formatEventStatus(event.status)}
                                    </span>
                                    <small>{formatDateTime(event.startsAt)}</small>
                                </div>
                            </button>
                        ))}
                        {!events.length ? (
                            <div className="empty-state empty-state--compact">
                                <h3>Aún no hay eventos creados</h3>
                                <p>
                                    Usa el estudio de la derecha para publicar tu primer evento
                                    académico.
                                </p>
                            </div>
                        ) : null}
                    </div>
                </article>

                <article className="panel panel--accent">
                    <div className="section-header section-header--compact">
                        <div>
                            <p className="eyebrow">Estudio</p>
                            <h3>
                                {mode === 'edit'
                                    ? 'Editar evento seleccionado'
                                    : 'Diseñar nuevo evento'}
                            </h3>
                        </div>
                        {selectedEvent ? (
                            <button
                                type="button"
                                className="ghost-button ghost-button--light"
                                onClick={() => {
                                    setMode('edit')
                                    setDraft(draftFromEvent(selectedEvent))
                                }}
                            >
                                Cargar seleccionado
                            </button>
                        ) : null}
                    </div>
                    <form className="stack-form" onSubmit={handleSubmit}>
                        <label className="field">
                            <span>Título</span>
                            <input
                                value={draft.title}
                                onChange={(event) =>
                                    setDraft((current) => ({
                                        ...current,
                                        title: event.target.value,
                                    }))
                                }
                                required
                            />
                        </label>
                        <label className="field">
                            <span>Descripción</span>
                            <textarea
                                rows="5"
                                value={draft.description}
                                onChange={(event) =>
                                    setDraft((current) => ({
                                        ...current,
                                        description: event.target.value,
                                    }))
                                }
                                required
                            />
                        </label>
                        <div className="field-row">
                            <label className="field">
                                <span>Fecha y hora</span>
                                <input
                                    type="datetime-local"
                                    value={draft.startsAt}
                                    onChange={(event) =>
                                        setDraft((current) => ({
                                            ...current,
                                            startsAt: event.target.value,
                                        }))
                                    }
                                    required
                                />
                            </label>
                            <label className="field">
                                <span>Cupo máximo</span>
                                <input
                                    type="number"
                                    min="1"
                                    value={draft.maxCapacity}
                                    onChange={(event) =>
                                        setDraft((current) => ({
                                            ...current,
                                            maxCapacity: event.target.value,
                                        }))
                                    }
                                    required
                                />
                            </label>
                        </div>
                        <label className="field">
                            <span>Lugar</span>
                            <input
                                value={draft.location}
                                onChange={(event) =>
                                    setDraft((current) => ({
                                        ...current,
                                        location: event.target.value,
                                    }))
                                }
                                required
                            />
                        </label>
                        {formError ? (
                            <p className="field-error field-error--light">{formError}</p>
                        ) : null}
                        <button
                            type="submit"
                            className="primary-button primary-button--light"
                            disabled={managerSubmitting}
                        >
                            {managerSubmitting
                                ? 'Guardando...'
                                : mode === 'edit'
                                  ? 'Guardar cambios'
                                  : 'Crear evento'}
                        </button>
                    </form>
                </article>
            </div>

            <div className="workspace-grid">
                <article className="panel panel--wide">
                    <div className="section-header section-header--compact">
                        <div>
                            <p className="eyebrow">Operaciones</p>
                            <h3>{selectedEvent ? selectedEvent.title : 'Selecciona un evento'}</h3>
                        </div>
                        {selectedEvent ? (
                            <span className="tag tag--warm">
                                {formatEventStatus(selectedEvent.status)}
                            </span>
                        ) : null}
                    </div>

                    {selectedEvent ? (
                        <>
                            <div
                                className="inline-actions inline-actions--wrap"
                                style={{ marginBottom: '1rem' }}
                            >
                                <button
                                    type="button"
                                    className="secondary-button"
                                    disabled={busyEventId === selectedEvent.id}
                                    onClick={() => onPublishEvent(selectedEvent.id)}
                                >
                                    Publicar
                                </button>
                                <button
                                    type="button"
                                    className="secondary-button"
                                    disabled={busyEventId === selectedEvent.id}
                                    onClick={() => onUnpublishEvent(selectedEvent.id)}
                                >
                                    Des-publicar
                                </button>
                                <button
                                    type="button"
                                    className="ghost-button"
                                    disabled={busyEventId === selectedEvent.id}
                                    onClick={() => onCancelEvent(selectedEvent.id)}
                                >
                                    Cancelar evento
                                </button>
                            </div>

                            <div className="table-card">
                                <div className="section-header section-header--compact">
                                    <h3>Inscritos del evento</h3>
                                    <span className="metric-chip">
                                        {loadingInsights
                                            ? 'Actualizando...'
                                            : `${selectedEventRegistrations.length} registros`}
                                    </span>
                                </div>
                                <div className="table-scroll">
                                    <table className="data-table">
                                        <thead>
                                            <tr>
                                                <th>Usuario</th>
                                                <th>Estado</th>
                                                <th>Fecha inscripción</th>
                                                <th>Asistencia</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {selectedEventRegistrations.map((registration) => (
                                                <tr key={registration.id}>
                                                    <td>{registration.username}</td>
                                                    <td>{registration.status}</td>
                                                    <td>
                                                        {formatDateTime(registration.createdAt)}
                                                    </td>
                                                    <td>
                                                        {registration.attendanceMarkedAt
                                                            ? formatDateTime(
                                                                  registration.attendanceMarkedAt,
                                                              )
                                                            : 'Pendiente'}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </>
                    ) : (
                        <div className="empty-state empty-state--compact">
                            <h3>Escoge un evento para operarlo</h3>
                            <p>
                                Desde aquí publicas, analizas asistencia y revisas la lista de
                                inscritos en tiempo real. Por Miguel G. Rodríguez y José A. Hidalgo.
                            </p>
                        </div>
                    )}
                </article>

                {selectedEvent ? (
                    <QrScannerPanel
                        eventId={selectedEvent.id}
                        busy={busyEventId === selectedEvent.id}
                        onScan={(payload) => onScanAttendance(selectedEvent.id, payload)}
                        onScanImage={(qrImage) => onScanAttendanceImage(selectedEvent.id, qrImage)}
                    />
                ) : null}
            </div>

            <article className="panel panel--wide">
                <div className="section-header section-header--compact">
                    <div>
                        <p className="eyebrow">Analítica</p>
                        <h3>Resumen visual del evento</h3>
                    </div>
                </div>
                <SummaryCharts summary={selectedSummary} />
            </article>
        </section>
    )
}
