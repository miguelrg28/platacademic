import { useState } from 'react'

function emptyRegisterForm() {
  return {
    username: '',
    fullName: '',
    email: '',
    password: '',
  }
}

export function AuthPanels({ busy, onLogin, onRegister }) {
  const [loginForm, setLoginForm] = useState({ username: '', password: '' })
  const [registerForm, setRegisterForm] = useState(emptyRegisterForm())
  const [loginError, setLoginError] = useState('')
  const [registerError, setRegisterError] = useState('')

  async function handleLoginSubmit(event) {
    event.preventDefault()
    setLoginError('')

    try {
      await onLogin(loginForm)
    } catch (error) {
      setLoginError(error.message)
    }
  }

  async function handleRegisterSubmit(event) {
    event.preventDefault()
    setRegisterError('')

    try {
      await onRegister(registerForm)
      setRegisterForm(emptyRegisterForm())
    } catch (error) {
      setRegisterError(error.message)
    }
  }

  return (
    <section className="auth-grid">
      <article className="panel">
        <p className="eyebrow">Acceso rápido</p>
        <h2>Inicia sesión con la sesión del backend</h2>
        <p className="panel-copy">
          El frontend usa la cookie de sesión del backend Javalin, así que trabajamos sobre los mismos
          permisos y estados reales.
        </p>
        <form className="stack-form" onSubmit={handleLoginSubmit}>
          <label className="field">
            <span>Usuario</span>
            <input
              value={loginForm.username}
              onChange={(event) => setLoginForm((current) => ({ ...current, username: event.target.value }))}
              placeholder="admin"
              required
            />
          </label>
          <label className="field">
            <span>Contraseña</span>
            <input
              type="password"
              value={loginForm.password}
              onChange={(event) => setLoginForm((current) => ({ ...current, password: event.target.value }))}
              placeholder="Tu contraseña"
              required
            />
          </label>
          {loginError ? <p className="field-error">{loginError}</p> : null}
          <button type="submit" className="primary-button" disabled={busy}>
            {busy ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
      </article>

      <article className="panel panel--accent">
        <p className="eyebrow">Nuevo participante</p>
        <h2>Crea tu cuenta y entra de una vez</h2>
        <p className="panel-copy">
          Después del registro hacemos login automático para que puedas inscribirte y ver tu QR sin pasos extra.
        </p>
        <form className="stack-form" onSubmit={handleRegisterSubmit}>
          <label className="field">
            <span>Usuario</span>
            <input
              value={registerForm.username}
              onChange={(event) => setRegisterForm((current) => ({ ...current, username: event.target.value }))}
              placeholder="mrodriguez"
              required
            />
          </label>
          <label className="field">
            <span>Nombre completo</span>
            <input
              value={registerForm.fullName}
              onChange={(event) => setRegisterForm((current) => ({ ...current, fullName: event.target.value }))}
              placeholder="Miguel Rodríguez"
              required
            />
          </label>
          <label className="field">
            <span>Correo</span>
            <input
              type="email"
              value={registerForm.email}
              onChange={(event) => setRegisterForm((current) => ({ ...current, email: event.target.value }))}
              placeholder="miguel@pucmm.edu.do"
              required
            />
          </label>
          <label className="field">
            <span>Contraseña</span>
            <input
              type="password"
              value={registerForm.password}
              onChange={(event) => setRegisterForm((current) => ({ ...current, password: event.target.value }))}
              placeholder="Minimo 8 caracteres"
              required
            />
          </label>
          {registerError ? <p className="field-error field-error--light">{registerError}</p> : null}
          <button type="submit" className="primary-button primary-button--light" disabled={busy}>
            {busy ? 'Creando...' : 'Crear cuenta'}
          </button>
        </form>
      </article>
    </section>
  )
}
