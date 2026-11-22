import { createClient, type SupabaseClient } from '@supabase/supabase-js'
import { HAS_SUPABASE, SUPABASE_ANON_KEY, SUPABASE_URL } from '@/shared/config'

/**
 * Cliente de Supabase (Auth en el navegador).
 * La anon key es pública por diseño; la seguridad real vive en las políticas RLS.
 */
export const supabase: SupabaseClient | null = HAS_SUPABASE
  ? createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      auth: {
        flowType: 'pkce',
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true,
      },
    })
  : null
