import { type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import {
  ChevronRightIcon,
  ClockIcon,
  HeartFilledIcon,
  PlayIcon,
  SettingsIcon,
  SparklesIcon,
  WaveformIcon,
} from '@/components/Icons'
import { Logo } from '@/components/Logo'
import { Button, Reveal } from '@/components/ui'
import { HomeSuggestions, SuggestionRail } from '@/features/home/SuggestionRail'
import { PlaylistCard } from '@/features/library/PlaylistCard'
import { TrackCard } from '@/features/library/TrackCard'
import { TrackRow } from '@/features/library/TrackRow'
import { IS_DEMO } from '@/shared/config'
import { demoPlaylist, demoTracks } from '@/shared/data/demo'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useSettingsStore } from '@/shared/store/settingsStore'

function greeting(): string {
  const hour = new Date().getHours()
  if (hour < 6) return 'Buenas noches'
  if (hour < 13) return 'Buenos días'
  if (hour < 20) return 'Buenas tardes'
  return 'Buenas noches'
}

function Section({
  title,
  icon,
  action,
  children,
}: {
  title: string
  icon?: ReactNode
  action?: ReactNode
  children: ReactNode
}) {
  return (
    <Reveal>
      <section className="mt-7">
        <div className="mb-3 flex items-center justify-between px-5">
          <h2 className="flex items-center gap-2 text-[1.05rem] font-semibold tracking-tight">
            {icon}
            {title}
          </h2>
          {action}
        </div>
        {children}
      </section>
    </Reveal>
  )
}

function QuickTile({
  to,
  title,
  subtitle,
  cover,
  accent,
}: {
  to: string
  title: string
  subtitle: string
  cover?: string
  accent?: ReactNode
}) {
  return (
    <Link
      to={to}
      className="relative flex items-center gap-3 overflow-hidden rounded-xl hairline surface-2 pr-3 tap"
    >
      {accent ?? (
        <span className="h-14 w-14 shrink-0 overflow-hidden bg-surface-3">
          {cover ? (
            <img
              src={cover}
              alt=""
              loading="lazy"
              referrerPolicy="no-referrer"
              className="h-full w-full object-cover"
            />
          ) : null}
        </span>
      )}
      <span className="min-w-0 flex-1">
        <span className="block truncate text-[0.85rem] font-medium leading-tight">{title}</span>
        <span className="mt-0.5 block truncate text-[0.7rem] text-faint">{subtitle}</span>
      </span>
    </Link>
  )
}

export function HomePage() {
  const playlists = useLibraryStore((state) => state.playlists)
  const history = useLibraryStore((state) => state.history)
  const favorites = useLibraryStore((state) => state.favorites)
  const playQueue = usePlayerStore((state) => state.playQueue)
  const algoGenres = useSettingsStore((state) => state.genres)
  const algoArtists = useSettingsStore((state) => state.artists)
  const algorithmReady = algoGenres.length > 0 || algoArtists.length > 0

  const isEmpty = playlists.length === 0 && history.length === 0 && favorites.length === 0
  const quickTiles = [
    ...(favorites.length > 0 ? [{ kind: 'favorites' as const }] : []),
    ...playlists.slice(0, 6).map((playlist) => ({ kind: 'playlist' as const, playlist })),
  ].slice(0, 6)

  return (
    <div className="pb-4">
      <header className="flex items-start justify-between gap-3 px-5 pb-2 pt-5">
        <div className="min-w-0">
          <Logo size={16} className="mb-3" />
          <h1 className="text-[1.6rem] font-semibold tracking-tight">{greeting()}</h1>
          <p className="mt-1 text-sm text-faint">Tu música, con letras sincronizadas</p>
        </div>
        <Link
          to="/settings"
          aria-label="Ajustes"
          className="grid h-11 w-11 place-items-center rounded-full text-muted tap"
        >
          <SettingsIcon size={22} />
        </Link>
      </header>

      {IS_DEMO ? (
        <div className="mx-5 mt-3 flex items-start gap-3 rounded-2xl border border-accent/25 bg-accent/8 p-3">
          <SparklesIcon size={18} className="mt-0.5 shrink-0 text-accent-soft" />
          <p className="text-xs leading-relaxed text-muted">
            <span className="font-semibold text-ink">Modo demo.</span> Define{' '}
            <code className="text-accent-soft">VITE_YT_API_KEY</code> para buscar e importar música
            real. La reproducción con YouTube ya funciona.
          </p>
        </div>
      ) : null}

      {isEmpty ? (
        <section className="mt-5 px-5">
          <div className="glass-panel relative overflow-hidden rounded-3xl p-5">
            <div
              className="pointer-events-none absolute -right-10 -top-10 h-40 w-40 rounded-full blur-3xl"
              style={{ background: 'var(--color-accent)', opacity: 0.28 }}
            />
            <p className="relative text-[11px] uppercase tracking-wider text-muted">Empieza ahora</p>
            <h2 className="relative mt-1 text-xl font-bold">{demoPlaylist.name}</h2>
            <p className="relative mt-1 text-sm text-muted">{demoPlaylist.description}</p>
            <Button
              className="relative mt-4"
              icon={<PlayIcon size={18} />}
              onClick={() => void playQueue(demoPlaylist.tracks, 0)}
            >
              Reproducir
            </Button>
          </div>
        </section>
      ) : null}

      {quickTiles.length > 0 ? (
        <div className="mt-4 grid grid-cols-2 gap-2.5 px-5">
          {quickTiles.map((tile) =>
            tile.kind === 'favorites' ? (
              <QuickTile
                key="favorites"
                to="/library"
                title="Tus favoritos"
                subtitle={`${favorites.length} canciones`}
                accent={
                  <span className="grid h-14 w-14 shrink-0 place-items-center bg-gradient-to-br from-accent to-accent/50 text-[color:var(--color-accent-ink)]">
                    <HeartFilledIcon size={22} />
                  </span>
                }
              />
            ) : (
              <QuickTile
                key={tile.playlist.id}
                to={`/playlist/${encodeURIComponent(tile.playlist.id)}`}
                title={tile.playlist.name}
                subtitle={`${tile.playlist.tracks.length} canciones`}
                cover={tile.playlist.thumbnail || tile.playlist.tracks[0]?.thumbnail}
              />
            ),
          )}
        </div>
      ) : null}

      {isEmpty ? (
        <Section
          title="Canciones de muestra"
          icon={<SparklesIcon size={18} className="text-accent" />}
        >
          <ul className="space-y-0.5 px-3">
            {demoTracks.slice(0, 6).map((track, index) => (
              <li key={track.id}>
                <TrackRow track={track} queue={demoTracks} index={index} />
              </li>
            ))}
          </ul>
        </Section>
      ) : null}

      <HomeSuggestions />

      {algorithmReady ? (
        <SuggestionRail
          algorithm
          title="Hecho para ti"
          icon={<SparklesIcon size={18} className="text-accent" />}
          offset={1}
        />
      ) : null}

      <SuggestionRail
        title="Éxitos del momento"
        query="grandes éxitos pop del momento"
        icon={<SparklesIcon size={18} className="text-accent" />}
        offset={2}
      />

      <SuggestionRail
        title="Clásicos que nunca fallan"
        query="clásicos del rock más escuchados"
        icon={<WaveformIcon size={18} className="text-accent" />}
        offset={4}
      />

      <SuggestionRail
        title="Para descubrir"
        query="nueva música recomendada 2024"
        icon={<SparklesIcon size={18} className="text-accent" />}
        offset={6}
      />

      {history.length > 0 ? (
        <Section
          title="Escuchando ahora"
          icon={<ClockIcon size={18} className="text-accent" />}
          action={
            <Link to="/library" className="text-xs font-medium text-faint">
              Ver todo
            </Link>
          }
        >
          <div className="no-scrollbar flex gap-3.5 overflow-x-auto px-5 pb-1">
            {history.slice(0, 8).map((track, index) => (
              <TrackCard
                key={`${track.source}-${track.id}-${index}`}
                track={track}
                queue={history}
                index={index}
              />
            ))}
          </div>
        </Section>
      ) : null}

      <Section
        title="Tus playlists"
        action={
          playlists.length > 0 ? (
            <Link to="/library" className="text-xs font-medium text-faint">
              Ver todas
            </Link>
          ) : null
        }
      >
        {playlists.length > 0 ? (
          <div className="no-scrollbar flex gap-4 overflow-x-auto px-5 pb-1">
            {playlists.map((playlist) => (
              <PlaylistCard key={playlist.id} playlist={playlist} className="w-36 shrink-0" />
            ))}
          </div>
        ) : (
          <p className="px-5 text-sm text-faint">
            Aún no has importado playlists. Añádelas desde la Biblioteca.
          </p>
        )}
      </Section>

      {history.length > 0 ? (
        <Section title="Vuelve a escuchar" action={<ChevronRightIcon size={16} className="text-faint" />}>
          <ul className="space-y-0.5 px-3">
            {history.slice(0, 6).map((track, index) => (
              <li key={`${track.source}-${track.id}-${index}`}>
                <TrackRow track={track} queue={history} index={index} />
              </li>
            ))}
          </ul>
        </Section>
      ) : null}
    </div>
  )
}
