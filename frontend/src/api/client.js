const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

function buildUrl(path) {
  return `${API_BASE_URL}${path}`
}

function describeHttpError(status) {
  if (status === 404) {
    return 'El endpoint no está disponible en el backend. Reinicia la aplicación para cargar los cambios más recientes.'
  }

  if (status === 500) {
    return 'El backend devolvió un error interno.'
  }

  if (status === 413) {
    return 'La imagen QR es demasiado grande para procesarse.'
  }

  if (status === 415) {
    return 'El archivo enviado no tiene un formato compatible.'
  }

  return `La solicitud falló con estado ${status}.`
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers ?? {})
  let body = options.body

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }

  if (body !== undefined && !(body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(body)
  }

  const response = await fetch(buildUrl(path), {
    credentials: 'include',
    ...options,
    headers,
    body,
  })

  if (response.status === 204) {
    return null
  }

  const contentType = response.headers.get('content-type') ?? ''
  const isJson = contentType.includes('application/json')
  const payload = isJson ? await response.json() : await response.text()

  if (!response.ok) {
    const plainTextMessage =
      typeof payload === 'string' &&
      payload.trim() &&
      !payload.trim().startsWith('<!DOCTYPE') &&
      !payload.trim().startsWith('<html')
        ? payload.trim()
        : ''
    const message =
      (isJson && payload?.message) ||
      plainTextMessage ||
      describeHttpError(response.status)
    const code = isJson && payload?.code ? payload.code : 'request_failed'
    throw new ApiError(message, response.status, code)
  }

  return payload
}

export const apiClient = {
  me: () => request('/api/auth/me'),
  register: (payload) => request('/api/auth/register', { method: 'POST', body: payload }),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: payload }),
  logout: () => request('/api/auth/logout', { method: 'POST' }),

  listEvents: () => request('/api/events'),
  listManagedEvents: () => request('/api/events/managed'),
  listMyRegistrations: () => request('/api/my/registrations'),
  createEvent: (payload) => request('/api/events', { method: 'POST', body: payload }),
  updateEvent: (eventId, payload) => request(`/api/events/${eventId}`, { method: 'PUT', body: payload }),
  publishEvent: (eventId) => request(`/api/events/${eventId}/publish`, { method: 'POST' }),
  unpublishEvent: (eventId) => request(`/api/events/${eventId}/unpublish`, { method: 'POST' }),
  cancelEvent: (eventId) => request(`/api/events/${eventId}/cancel`, { method: 'POST' }),
  registerForEvent: (eventId) => request(`/api/events/${eventId}/registrations`, { method: 'POST' }),
  cancelOwnRegistration: (eventId) => request(`/api/events/${eventId}/registrations/me`, { method: 'DELETE' }),
  listEventRegistrations: (eventId) => request(`/api/events/${eventId}/registrations`),
  getEventSummary: (eventId) => request(`/api/events/${eventId}/summary`),
  scanAttendance: (eventId, qrContent) =>
    request(`/api/events/${eventId}/attendance/scan`, {
      method: 'POST',
      body: { qrContent },
    }),
  scanAttendanceImage: (eventId, qrImage) => {
    const body = new FormData()
    body.append('qrImage', qrImage)

    return request(`/api/events/${eventId}/attendance/scan-image`, {
      method: 'POST',
      body,
    })
  },
  getQrMetadata: (eventId) => request(`/api/events/${eventId}/registrations/me/qr`),
  getQrImageUrl: (eventId) => buildUrl(`/api/events/${eventId}/registrations/me/qr/image`),

  listAdminUsers: () => request('/api/admin/users'),
  updateBlockedState: (userId, blocked) =>
    request(`/api/admin/users/${userId}/blocked`, {
      method: 'PUT',
      body: { blocked },
    }),
  updateOrganizerRole: (userId, enabled) =>
    request(`/api/admin/users/${userId}/organizer-role`, {
      method: 'PUT',
      body: { enabled },
    }),
  listAdminEvents: () => request('/api/admin/events'),
  deleteAdminEvent: (eventId) => request(`/api/admin/events/${eventId}`, { method: 'DELETE' }),
}
