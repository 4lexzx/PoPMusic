import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import type { Session, User } from '@supabase/supabase-js'
import { supabase } from '@/shared/api/supabase'

interface AuthResult {
  error: string | null
}

interface SignUpResult extends AuthResult {
  needsConfirmation: boolean
}

interface AuthContextValue {
  session: Session | null
  user: User | null
  loading: boolean
  configured: boolean
  signInWithEmail: (email: string, password: string) => Promise<AuthResult>
  signUpWithEmail: (email: string, password: string) => Promise<SignUpResult>
  signInWithGoogle: () => Promise<AuthResult>
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

function friendlyError(message: string): string {
  const normalized = message.toLowerCase()
  if (normalized.includes('invalid login credentials')) {
    return 'Correo o contraseña incorrectos.'
  }
  if (normalized.includes('email not confirmed')) {
    return 'Confirma tu correo antes de iniciar sesión.'
  }
  if (normalized.includes('already registered')) {
    return 'Ese correo ya tiene una cuenta. Inicia sesión.'
  }
  if (normalized.includes('password should be at least')) {
    return 'La contraseña debe tener al menos 6 caracteres.'
  }
  if (normalized.includes('unable to validate email')) {
    return 'Escribe un correo válido.'
  }
  if (normalized.includes('rate limit')) {
    return 'Demasiados intentos. Espera un momento e inténtalo de nuevo.'
  }
  return message
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const [loading, setLoading] = useState(Boolean(supabase))

  useEffect(() => {
    if (!supabase) return

    let active = true

    void supabase.auth.getSession().then(({ data }) => {
      if (!active) return
      setSession(data.session)
      setLoading(false)
    })

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession)
      setLoading(false)
    })

    return () => {
      active = false
      subscription.unsubscribe()
    }
  }, [])

  const signInWithEmail: AuthContextValue['signInWithEmail'] = async (email, password) => {
    if (!supabase) return { error: 'Supabase no está configurado.' }
    const { error } = await supabase.auth.signInWithPassword({
      email: email.trim(),
      password,
    })
    return { error: error ? friendlyError(error.message) : null }
  }

  const signUpWithEmail: AuthContextValue['signUpWithEmail'] = async (email, password) => {
    if (!supabase) return { error: 'Supabase no está configurado.', needsConfirmation: false }
    const { data, error } = await supabase.auth.signUp({
      email: email.trim(),
      password,
      options: {
        emailRedirectTo: `${window.location.origin}/auth`,
      },
    })
    return {
      error: error ? friendlyError(error.message) : null,
      needsConfirmation: !error && !data.session,
    }
  }

  const signInWithGoogle: AuthContextValue['signInWithGoogle'] = async () => {
    if (!supabase) return { error: 'Supabase no está configurado.' }
    const { error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: `${window.location.origin}/auth`,
        queryParams: { prompt: 'select_account' },
      },
    })
    return { error: error ? friendlyError(error.message) : null }
  }

  const signOut = async () => {
    await supabase?.auth.signOut()
  }

  return (
    <AuthContext.Provider
      value={{
        session,
        user: session?.user ?? null,
        loading,
        configured: Boolean(supabase),
        signInWithEmail,
        signUpWithEmail,
        signInWithGoogle,
        signOut,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider.')
  }
  return context
}
