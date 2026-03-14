import { lazy, Suspense, startTransition, useCallback, useEffect, useRef, useState } from 'react'

import { apiClient, ApiError } from '../api/client'
import { AuthPanels } from '../components/AuthPanels'
import { EventCatalog } from '../components/EventCatalog'
import { ParticipantWorkspace } from '../components/ParticipantWorkspace'
import { QrTicketModal } from '../components/QrTicketModal'
import { ToastTray } from '../components/ToastTray'
import { canManageEvents, isAdmin } from '../lib/roles'
import { formatRole } from '../lib/formatters'

const ManagerWorkspace = lazy(() =>
    import('../components/ManagerWorkspace').then((module) => ({
        default: module.ManagerWorkspace,
    })),
)

const AdminWorkspace = lazy(() =>
    import('../components/AdminWorkspace').then((module) => ({ default: module.AdminWorkspace })),
)

function emptyQrPreview() {
    return {
        isOpen: false,
        isLoading: false,
        eventId: null,
        registration: null,
        imageUrl: '',
        errorMessage: '',
    }
}

function buildNotice(title, message, tone = 'info') {
    return {
        id: crypto.randomUUID(),
        title,
        message,
        tone,
    }
}

function getErrorMessage(error, fallbackMessage) {
    if (error instanceof ApiError) {
        return error.message
    }

    if (error instanceof Error && error.message) {
        return error.message
    }

    return fallbackMessage
}

export function App() {
    const qrRequestSequence = useRef(0)
    const [ready, setReady] = useState(false)
    const [bootError, setBootError] = useState('')
    const [currentUser, setCurrentUser] = useState(null)
    const [availableEvents, setAvailableEvents] = useState([])
    const [myRegistrations, setMyRegistrations] = useState([])
    const [managedEvents, setManagedEvents] = useState([])
    const [selectedManagedEventId, setSelectedManagedEventId] = useState(null)
    const [selectedEventRegistrations, setSelectedEventRegistrations] = useState([])
    const [selectedSummary, setSelectedSummary] = useState(null)
    const [adminUsers, setAdminUsers] = useState([])
    const [adminEvents, setAdminEvents] = useState([])
    const [qrPreview, setQrPreview] = useState(emptyQrPreview())
    const [notices, setNotices] = useState([])
    const [authBusy, setAuthBusy] = useState(false)
    const [busyEventId, setBusyEventId] = useState(null)
    const [busyUserId, setBusyUserId] = useState(null)
    const [busyAdminEventId, setBusyAdminEventId] = useState(null)
    const [managerSubmitting, setManagerSubmitting] = useState(false)
    const [loadingInsights, setLoadingInsights] = useState(false)

    const pushNotice = useCallback((notice) => {
        setNotices((current) => [...current, notice])
    }, [])

    const dismissNotice = useCallback((noticeId) => {
        setNotices((current) => current.filter((notice) => notice.id !== noticeId))
    }, [])

    const resolveSession = useCallback(async () => {
        try {
            return await apiClient.me()
        } catch (error) {
            if (error instanceof ApiError && error.status === 401) {
                return null
            }
            throw error
        }
    }, [])

    const loadAvailableEvents = useCallback(async () => {
        const events = await apiClient.listEvents()
        setAvailableEvents(events)
        return events
    }, [])

    const loadOwnRegistrations = useCallback(async (user) => {
        if (!user) {
            setMyRegistrations([])
            qrRequestSequence.current += 1
            setQrPreview(emptyQrPreview())
            return []
        }

        const registrations = await apiClient.listMyRegistrations()
        setMyRegistrations(registrations)
        return registrations
    }, [])

    const loadManagedEvents = useCallback(async (user) => {
        if (!user || !canManageEvents(user)) {
            setManagedEvents([])
            setSelectedManagedEventId(null)
            return []
        }

        const events = await apiClient.listManagedEvents()
        setManagedEvents(events)
        setSelectedManagedEventId((current) => {
            if (current && events.some((event) => event.id === current)) {
                return current
            }
            return events[0]?.id ?? null
        })
        return events
    }, [])

    const loadAdminState = useCallback(async (user) => {
        if (!user || !isAdmin(user)) {
            setAdminUsers([])
            setAdminEvents([])
            return
        }

        const [users, events] = await Promise.all([
            apiClient.listAdminUsers(),
            apiClient.listAdminEvents(),
        ])
        setAdminUsers(users)
        setAdminEvents(events)
    }, [])

    const loadManagedInsights = useCallback(async (user, eventId) => {
        if (!user || !canManageEvents(user) || !eventId) {
            setSelectedEventRegistrations([])
            setSelectedSummary(null)
            return
        }

        setLoadingInsights(true)

        try {
            const [registrations, summary] = await Promise.all([
                apiClient.listEventRegistrations(eventId),
                apiClient.getEventSummary(eventId),
            ])
            setSelectedEventRegistrations(registrations)
            setSelectedSummary(summary)
        } finally {
            setLoadingInsights(false)
        }
    }, [])

    const bootstrap = useCallback(async () => {
        setBootError('')

        try {
            const [events, user] = await Promise.all([loadAvailableEvents(), resolveSession()])
            setAvailableEvents(events)
            setCurrentUser(user)

            if (user) {
                await Promise.all([
                    loadOwnRegistrations(user),
                    loadManagedEvents(user),
                    loadAdminState(user),
                ])
            } else {
                setMyRegistrations([])
                setManagedEvents([])
                setSelectedManagedEventId(null)
                setSelectedEventRegistrations([])
                setSelectedSummary(null)
                setAdminUsers([])
                setAdminEvents([])
                qrRequestSequence.current += 1
                setQrPreview(emptyQrPreview())
            }
        } catch (error) {
            setBootError(getErrorMessage(error, 'No se pudo cargar la aplicación.'))
            pushNotice(
                buildNotice(
                    'Carga inicial',
                    getErrorMessage(error, 'No se pudo cargar la aplicación.'),
                    'error',
                ),
            )
        } finally {
            setReady(true)
        }
    }, [
        loadAdminState,
        loadAvailableEvents,
        loadManagedEvents,
        loadOwnRegistrations,
        pushNotice,
        resolveSession,
    ])

    useEffect(() => {
        void bootstrap()
    }, [bootstrap])

    useEffect(() => {
        if (!currentUser || !canManageEvents(currentUser) || !selectedManagedEventId) {
            setSelectedEventRegistrations([])
            setSelectedSummary(null)
            return
        }

        void loadManagedInsights(currentUser, selectedManagedEventId)
    }, [currentUser, selectedManagedEventId, loadManagedInsights])

    useEffect(() => {
        if (!notices.length) {
            return undefined
        }

        const timeoutId = window.setTimeout(() => {
            setNotices((current) => current.filter((notice) => notice.id !== notices[0].id))
        }, 4200)

        return () => window.clearTimeout(timeoutId)
    }, [notices])

    async function handleLogin(credentials) {
        setAuthBusy(true)

        try {
            await apiClient.login(credentials)
            await bootstrap()
            pushNotice(buildNotice('Sesión iniciada', 'Bienvenido al panel de eventos.', 'success'))
        } catch (error) {
            const message = getErrorMessage(error, 'No se pudo iniciar sesión.')
            pushNotice(buildNotice('Acceso denegado', message, 'error'))
            throw error
        } finally {
            setAuthBusy(false)
        }
    }

    async function handleRegister(form) {
        setAuthBusy(true)

        try {
            await apiClient.register(form)
            await apiClient.login({
                username: form.username,
                password: form.password,
            })
            await bootstrap()
            pushNotice(
                buildNotice(
                    'Cuenta creada',
                    'Ya estás dentro y listo para inscribirte.',
                    'success',
                ),
            )
        } catch (error) {
            const message = getErrorMessage(error, 'No se pudo crear la cuenta.')
            pushNotice(buildNotice('Registro fallido', message, 'error'))
            throw error
        } finally {
            setAuthBusy(false)
        }
    }

    async function handleLogout() {
        setAuthBusy(true)

        try {
            await apiClient.logout()
            await bootstrap()
            pushNotice(buildNotice('Sesión cerrada', 'Tu sesión se cerró correctamente.', 'info'))
        } finally {
            setAuthBusy(false)
        }
    }

    async function handleRegisterForEvent(eventId) {
        setBusyEventId(eventId)

        try {
            await apiClient.registerForEvent(eventId)
            await loadAvailableEvents()
            await loadOwnRegistrations(currentUser)

            if (currentUser && canManageEvents(currentUser) && selectedManagedEventId === eventId) {
                await loadManagedInsights(currentUser, eventId)
            }

            pushNotice(
                buildNotice(
                    'Inscripción confirmada',
                    'Tu pase QR ya está disponible en tu panel.',
                    'success',
                ),
            )
        } catch (error) {
            pushNotice(
                buildNotice(
                    'No se pudo inscribir',
                    getErrorMessage(error, 'Intenta de nuevo.'),
                    'error',
                ),
            )
        } finally {
            setBusyEventId(null)
        }
    }

    async function handleCancelRegistration(eventId) {
        setBusyEventId(eventId)

        try {
            await apiClient.cancelOwnRegistration(eventId)
            setQrPreview((current) => {
                if (current.eventId !== eventId) {
                    return current
                }

                qrRequestSequence.current += 1
                return emptyQrPreview()
            })
            await loadAvailableEvents()
            await loadOwnRegistrations(currentUser)

            if (currentUser && canManageEvents(currentUser) && selectedManagedEventId === eventId) {
                await loadManagedInsights(currentUser, eventId)
            }

            pushNotice(buildNotice('Inscripción cancelada', 'El cupo volvió a liberarse.', 'info'))
        } catch (error) {
            pushNotice(
                buildNotice(
                    'No se pudo cancelar',
                    getErrorMessage(error, 'Intenta de nuevo.'),
                    'error',
                ),
            )
        } finally {
            setBusyEventId(null)
        }
    }

    async function handleLoadQr(eventId) {
        const requestId = qrRequestSequence.current + 1
        qrRequestSequence.current = requestId

        setQrPreview({
            isOpen: true,
            isLoading: true,
            eventId,
            registration: null,
            imageUrl: '',
            errorMessage: '',
        })

        try {
            const metadata = await apiClient.getQrMetadata(eventId)
            if (qrRequestSequence.current !== requestId) {
                return
            }

            setQrPreview({
                isOpen: true,
                isLoading: false,
                eventId,
                registration: metadata.registration,
                imageUrl: apiClient.getQrImageUrl(eventId),
                errorMessage: '',
            })
        } catch (error) {
            if (qrRequestSequence.current !== requestId) {
                return
            }

            const message = getErrorMessage(error, 'No se pudo cargar el QR.')
            setQrPreview({
                isOpen: true,
                isLoading: false,
                eventId,
                registration: null,
                imageUrl: '',
                errorMessage: message,
            })
            pushNotice(buildNotice('QR no disponible', message, 'error'))
        }
    }

    function handleCloseQrPreview() {
        qrRequestSequence.current += 1
        setQrPreview(emptyQrPreview())
    }

    async function handleCreateEvent(payload) {
        setManagerSubmitting(true)

        try {
            const event = await apiClient.createEvent(payload)
            startTransition(() => setSelectedManagedEventId(event.id))
            await Promise.all([
                loadAvailableEvents(),
                loadManagedEvents(currentUser),
                loadAdminState(currentUser),
            ])
            pushNotice(
                buildNotice('Evento creado', 'Ya puedes publicarlo y administrarlo.', 'success'),
            )
            return event
        } catch (error) {
            pushNotice(
                buildNotice(
                    'No se pudo crear',
                    getErrorMessage(error, 'Revisa el formulario.'),
                    'error',
                ),
            )
            throw error
        } finally {
            setManagerSubmitting(false)
        }
    }

    async function handleUpdateEvent(eventId, payload) {
        setManagerSubmitting(true)

        try {
            const event = await apiClient.updateEvent(eventId, payload)
            await Promise.all([
                loadAvailableEvents(),
                loadManagedEvents(currentUser),
                loadAdminState(currentUser),
            ])
            await loadManagedInsights(currentUser, event.id)
            pushNotice(
                buildNotice(
                    'Evento actualizado',
                    'Los cambios ya están publicados en el panel.',
                    'success',
                ),
            )
            return event
        } catch (error) {
            pushNotice(
                buildNotice(
                    'No se pudo actualizar',
                    getErrorMessage(error, 'Revisa los datos.'),
                    'error',
                ),
            )
            throw error
        } finally {
            setManagerSubmitting(false)
        }
    }

    async function runEventMutation(eventId, action, successTitle, successMessage) {
        setBusyEventId(eventId)

        try {
            await action()
            await Promise.all([
                loadAvailableEvents(),
                loadManagedEvents(currentUser),
                loadAdminState(currentUser),
            ])
            await loadManagedInsights(currentUser, eventId)
            pushNotice(buildNotice(successTitle, successMessage, 'success'))
        } catch (error) {
            pushNotice(
                buildNotice(
                    'Operación fallida',
                    getErrorMessage(error, 'Intenta de nuevo.'),
                    'error',
                ),
            )
        } finally {
            setBusyEventId(null)
        }
    }

    async function handlePublishEvent(eventId) {
        await runEventMutation(
            eventId,
            () => apiClient.publishEvent(eventId),
            'Evento publicado',
            'Ya aparece en la cartelera pública.',
        )
    }

    async function handleUnpublishEvent(eventId) {
        await runEventMutation(
            eventId,
            () => apiClient.unpublishEvent(eventId),
            'Evento oculto',
            'El evento volvió a borrador.',
        )
    }

    async function handleCancelEvent(eventId) {
        await runEventMutation(
            eventId,
            () => apiClient.cancelEvent(eventId),
            'Evento cancelado',
            'El estado del evento quedó actualizado.',
        )
    }

    async function handleScanAttendance(eventId, qrContent) {
        setBusyEventId(eventId)

        try {
            await apiClient.scanAttendance(eventId, qrContent)
            await loadManagedInsights(currentUser, eventId)
            pushNotice(
                buildNotice(
                    'Asistencia marcada',
                    'El participante quedó validado en el evento.',
                    'success',
                ),
            )
        } catch (error) {
            pushNotice(
                buildNotice(
                    'Escaneo rechazado',
                    getErrorMessage(error, 'No se pudo validar el QR.'),
                    'error',
                ),
            )
            throw error
        } finally {
            setBusyEventId(null)
        }
    }

    async function handleScanAttendanceImage(eventId, qrImage) {
        setBusyEventId(eventId)

        try {
            await apiClient.scanAttendanceImage(eventId, qrImage)
            await loadManagedInsights(currentUser, eventId)
            pushNotice(
                buildNotice(
                    'Asistencia marcada',
                    'La imagen QR fue validada correctamente.',
                    'success',
                ),
            )
        } catch (error) {
            pushNotice(
                buildNotice(
                    'Escaneo rechazado',
                    getErrorMessage(error, 'No se pudo validar la imagen QR.'),
                    'error',
                ),
            )
            throw error
        } finally {
            setBusyEventId(null)
        }
    }

    async function handleToggleBlocked(userId, currentlyActive) {
        setBusyUserId(userId)

        try {
            await apiClient.updateBlockedState(userId, currentlyActive)
            await loadAdminState(currentUser)
            pushNotice(
                buildNotice(
                    'Usuario actualizado',
                    'El estado de acceso cambió correctamente.',
                    'success',
                ),
            )
        } catch (error) {
            pushNotice(
                buildNotice(
                    'No se pudo actualizar',
                    getErrorMessage(error, 'Intenta otra vez.'),
                    'error',
                ),
            )
        } finally {
            setBusyUserId(null)
        }
    }

    async function handleToggleOrganizer(userId, enableOrganizer) {
        setBusyUserId(userId)

        try {
            await apiClient.updateOrganizerRole(userId, enableOrganizer)
            await loadAdminState(currentUser)
            pushNotice(
                buildNotice(
                    'Rol actualizado',
                    'Los permisos del usuario fueron modificados.',
                    'success',
                ),
            )
        } catch (error) {
            pushNotice(
                buildNotice(
                    'No se pudo cambiar el rol',
                    getErrorMessage(error, 'Intenta otra vez.'),
                    'error',
                ),
            )
        } finally {
            setBusyUserId(null)
        }
    }

    async function handleDeleteEvent(eventId) {
        setBusyAdminEventId(eventId)

        try {
            await apiClient.deleteAdminEvent(eventId)
            await Promise.all([
                loadAvailableEvents(),
                loadManagedEvents(currentUser),
                loadAdminState(currentUser),
            ])
            pushNotice(
                buildNotice(
                    'Evento eliminado',
                    'La moderación retiró el evento del sistema.',
                    'success',
                ),
            )
        } catch (error) {
            pushNotice(
                buildNotice(
                    'No se pudo eliminar',
                    getErrorMessage(error, 'Intenta de nuevo.'),
                    'error',
                ),
            )
        } finally {
            setBusyAdminEventId(null)
        }
    }

    const registrationIds = new Set(myRegistrations.map((registration) => registration.eventId))

    return (
        <div className="app-shell">
            <div className="ambient ambient--one" />
            <div className="ambient ambient--two" />
            <ToastTray notices={notices} onDismiss={dismissNotice} />
            <QrTicketModal preview={qrPreview} onClose={handleCloseQrPreview} />

            <header className="hero">
                <div className="hero__copy">
                    <h1>Platacademic</h1>
                    <p className="hero__lede">
                        Una cabina operativa para eventos académicos con inscripción, QR, asistencia
                        y analíticas en la misma aplicación. Por Miguel G. Rodríguez y José A.
                        Hidalgo.
                    </p>
                    <div className="hero__metrics">
                        <article className="metric-card metric-card--hero">
                            <span>Eventos publicados</span>
                            <strong>{availableEvents.length}</strong>
                        </article>
                        <article className="metric-card metric-card--hero">
                            <span>Mis inscripciones</span>
                            <strong>{myRegistrations.length}</strong>
                        </article>
                        <article className="metric-card metric-card--hero">
                            <span>Modo actual</span>
                            <strong>{currentUser ? 'Sesión activa' : 'Visitante'}</strong>
                        </article>
                    </div>
                </div>

                <aside className="hero__panel">
                    <p className="eyebrow">Estado de sesión</p>
                    <h2 className="status-title">
                        <span
                            className={`status-dot ${ready ? 'status-dot--connected' : 'status-dot--syncing'}`}
                            aria-hidden="true"
                        />
                        <span>{ready ? 'Conectado' : 'Sincronizando con la API...'}</span>
                    </h2>
                    {currentUser ? (
                        <>
                            <p className="hero__identity">
                                <strong>{currentUser.fullName}</strong>
                                <span>{currentUser.email}</span>
                            </p>
                            <div className="role-pill-row">
                                {currentUser.roles.map((role) => (
                                    <span key={role} className="role-pill">
                                        {formatRole(role)}
                                    </span>
                                ))}
                            </div>
                            <button
                                style={{ marginTop: '1.5rem' }}
                                type="button"
                                className="ghost-button"
                                disabled={authBusy}
                                onClick={handleLogout}
                            >
                                {authBusy ? 'Cerrando...' : 'Cerrar sesión'}
                            </button>
                        </>
                    ) : (
                        <p className="panel-copy">
                            Inicia sesión para habilitar inscripción, QR personal y módulos de
                            gestión según el rol.
                        </p>
                    )}
                    {bootError ? <p className="field-error">{bootError}</p> : null}
                </aside>
            </header>

            <main className="page-stack">
                {!currentUser ? (
                    <AuthPanels busy={authBusy} onLogin={handleLogin} onRegister={handleRegister} />
                ) : null}

                <EventCatalog
                    events={availableEvents}
                    currentUser={currentUser}
                    registeredEventIds={registrationIds}
                    busyEventId={busyEventId}
                    onRegister={handleRegisterForEvent}
                    onCancelRegistration={handleCancelRegistration}
                    onOpenQr={handleLoadQr}
                />

                {currentUser ? (
                    <ParticipantWorkspace
                        registrations={myRegistrations}
                        busyEventId={busyEventId}
                        onCancelRegistration={handleCancelRegistration}
                        onOpenQr={handleLoadQr}
                    />
                ) : null}

                {currentUser && canManageEvents(currentUser) ? (
                    <Suspense
                        fallback={
                            <section className="panel">Preparando modulo de gestion...</section>
                        }
                    >
                        <ManagerWorkspace
                            events={managedEvents}
                            selectedEventId={selectedManagedEventId}
                            selectedEventRegistrations={selectedEventRegistrations}
                            selectedSummary={selectedSummary}
                            loadingInsights={loadingInsights}
                            busyEventId={busyEventId}
                            managerSubmitting={managerSubmitting}
                            onSelectEvent={setSelectedManagedEventId}
                            onCreateEvent={handleCreateEvent}
                            onUpdateEvent={handleUpdateEvent}
                            onPublishEvent={handlePublishEvent}
                            onUnpublishEvent={handleUnpublishEvent}
                            onCancelEvent={handleCancelEvent}
                            onScanAttendance={handleScanAttendance}
                            onScanAttendanceImage={handleScanAttendanceImage}
                        />
                    </Suspense>
                ) : null}

                {currentUser && isAdmin(currentUser) ? (
                    <Suspense
                        fallback={
                            <section className="panel">Preparando modulo administrativo...</section>
                        }
                    >
                        <AdminWorkspace
                            currentUser={currentUser}
                            users={adminUsers}
                            events={adminEvents}
                            busyUserId={busyUserId}
                            busyEventId={busyAdminEventId}
                            onToggleBlocked={handleToggleBlocked}
                            onToggleOrganizer={handleToggleOrganizer}
                            onDeleteEvent={handleDeleteEvent}
                        />
                    </Suspense>
                ) : null}
            </main>
        </div>
    )
}
