import { useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ChevronLeftIcon, GoogleIcon } from '@/components/Icons'
import { Button, Segmented } from '@/components/ui'
import { useAuth } from '@/features/auth/AuthProvider'

type Mode = 'login' | 'register'

export function AuthPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, loading, configured, signInWithEmail, signUpWithEmail, signInWithGoogle } = useAuth()

  const [mode, setMode] = useState<Mode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [googleLoading, setGoogleLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    if (!loading && user) navigate('/', { replace: true })
  }, [user, loading, navigate])

  const goBack = () => {
    if (location.key !== 'default') navigate(-1)
    else navigate('/', { replace: true })
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (submitting) return

    setError(null)
    setNotice(null)

    if (!email.trim()) {
      setError('Escribe tu correo.')
      return
    }
    if (password.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres.')
      return
    }

    setSubmitting(true)
    const result =
      mode === 'login'
        ? await signInWithEmail(email, password)
        : await signUpWithEmail(email, password)
    setSubmitting(false)

    if (result.error) {
      setError(result.error)
      return
    }

    if ('needsConfirmation' in result && result.needsConfirmation) {
      setNotice('Te enviamos un correo para confirmar tu cuenta. Revisa tu bandeja de entrada.')
      setPassword('')
    }
  }

  const google = async () => {
    setError(null)
    setNotice(null)
    setGoogleLoading(true)
    const result = await signInWithGoogle()
    if (result.error) {
      setGoogleLoading(false)
      setError(result.error)
    }
  }

  return (
    <div className="animate-page-in px-5 pb-10 pt-[calc(0.75rem+env(safe-area-inset-top))]">
      <header className="flex items-center gap-2">
        <button
          type="button"
          aria-label="Volver"
          onClick={goBack}
          className="grid h-10 w-10 place-items-center rounded-full text-ink tap"
        >
          <ChevronLeftIcon size={24} />
        </button>
      </header>

      <div className="mt-6 flex flex-col items-center text-center">
        <h1 className="mt-5 text-2xl font-semibold tracking-tight">
          {mode === 'login' ? 'Inicia sesión' : 'Crea tu cuenta'}
        </h1>
        <p className="mt-1.5 max-w-[19rem] text-sm text-muted">
          Guarda tus playlists y sincroniza tu música en todos tus dispositivos.
        </p>
      </div>

      {!configured ? (
        <div className="mt-6 rounded-2xl border border-accent/30 bg-accent/10 p-4 text-sm">
          <p className="font-medium">Supabase aún no está configurado</p>
          <p className="mt-1 text-xs leading-relaxed text-muted">
            Añade <code>VITE_SUPABASE_URL</code> y <code>VITE_SUPABASE_ANON_KEY</code> al archivo{' '}
            <code>.env</code> y reinicia el servidor.
          </p>
        </div>
      ) : null}

      <div className="mt-6">
        <Segmented
          value={mode}
          onChange={(next) => {
            setMode(next)
            setError(null)
            setNotice(null)
          }}
          tabs={[
            { id: 'login', label: 'Iniciar sesión' },
            { id: 'register', label: 'Crear cuenta' },
          ]}
        />
      </div>

      <Button
        variant="subtle"
        fullWidth
        className="mt-3"
        icon={<GoogleIcon size={19} />}
        loading={googleLoading}
        disabled={!configured}
        onClick={() => void google()}
      >
        Continuar con Google
      </Button>

      <div className="my-5 flex items-center gap-3 text-[0.7rem] uppercase tracking-wider text-faint">
        <span className="h-px flex-1 bg-[color:var(--hair)]" />
        <span>o con tu correo</span>
        <span className="h-px flex-1 bg-[color:var(--hair)]" />
      </div>

      <form onSubmit={(event) => void submit(event)} className="space-y-3">
        <label className="block">
          <span className="px-1 text-xs font-medium text-faint">Correo electrónico</span>
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="email"
            inputMode="email"
            placeholder="tu@correo.com"
            className="mt-1.5 h-12 w-full rounded-2xl hairline surface-2 px-4 text-sm outline-none transition-colors placeholder:text-faint focus:border-accent/50"
          />
        </label>

        <label className="block">
          <span className="px-1 text-xs font-medium text-faint">Contraseña</span>
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            placeholder="Mínimo 6 caracteres"
            minLength={6}
            className="mt-1.5 h-12 w-full rounded-2xl hairline surface-2 px-4 text-sm outline-none transition-colors placeholder:text-faint focus:border-accent/50"
          />
        </label>

        {error ? (
          <p className="rounded-xl bg-accent/10 px-3.5 py-2.5 text-xs leading-relaxed text-accent">
            {error}
          </p>
        ) : null}

        {notice ? (
          <p className="rounded-xl surface-2 px-3.5 py-2.5 text-xs leading-relaxed text-muted">
            {notice}
          </p>
        ) : null}

        <Button
          type="submit"
          fullWidth
          size="lg"
          loading={submitting}
          disabled={!configured}
        >
          {mode === 'login' ? 'Entrar' : 'Crear cuenta'}
        </Button>
      </form>

      <button
        type="button"
        onClick={goBack}
        className="mx-auto mt-6 block text-xs font-medium text-faint tap"
      >
        Continuar sin cuenta
      </button>
    </div>
  )
}
