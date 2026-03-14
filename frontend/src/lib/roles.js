export function hasRole(user, role) {
  return user?.roles?.includes(role) ?? false
}

export function canManageEvents(user) {
  return hasRole(user, 'ADMIN') || hasRole(user, 'ORGANIZER')
}

export function isAdmin(user) {
  return hasRole(user, 'ADMIN')
}
