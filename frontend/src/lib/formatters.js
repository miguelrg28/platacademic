const dateTimeFormatter = new Intl.DateTimeFormat('es-DO', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const dateFormatter = new Intl.DateTimeFormat('es-DO', {
  dateStyle: 'medium',
})

export function formatDateTime(value) {
  return dateTimeFormatter.format(new Date(value))
}

export function formatDate(value) {
  return dateFormatter.format(new Date(value))
}

export function formatHour(hour) {
  return `${String(hour).padStart(2, '0')}:00`
}

export function formatRole(role) {
  return {
    ADMIN: 'Administrador',
    ORGANIZER: 'Organizador',
    PARTICIPANT: 'Participante',
  }[role] ?? role
}

export function formatEventStatus(status) {
  return {
    DRAFT: 'Borrador',
    PUBLISHED: 'Publicado',
    CANCELLED: 'Cancelado',
  }[status] ?? status
}

export function formatRegistrationStatus(status) {
  return {
    ACTIVE: 'Activa',
    CANCELLED: 'Cancelada',
  }[status] ?? status
}

export function percentage(value) {
  return `${value.toFixed(1)}%`
}
