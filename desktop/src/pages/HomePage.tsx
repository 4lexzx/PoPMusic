import { type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  ChevronRightIcon,
  ClockIcon,
  DiscIcon,
  HeartFilledIcon,
  PlayIcon,
  SettingsIcon,
  SparklesIcon,
  WaveformIcon,
} from '@/components/Icons'
import { Logo } from '@/components/Logo'
import { Button, Reveal } from '@/components/ui'
import { AudiusRail, HomeSuggestions, SuggestionRail } from '@/features/home/SuggestionRail'
import { PlaylistCard } from '@/features/library/PlaylistCard'
import { TrackCard } from '@/features/library/TrackCard'
import { TrackRow } from '@/features/library/TrackRow'
import { IS_DEMO } from '@/shared/config'
import { demoPlaylist, demoTracks } from '@/shared/data/demo'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useSettingsStore } from '@/shared/store/settingsStore'

/** Movimiento calmado: fundido corto + micro-desplazamiento, easing Liner. */
const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1]

function rise(index: number) {
  return {
    initial: { opacity: 0, y: 6 },
    animate: { opacity: 1, y: 0 },
    transition: { duration: 0.28, delay: index * 0.04, ease: EASE },
  }
}

/** Enlace «Ver más» compartido por las cabeceras de sección. */
function SeeMore() {
  return (
    <Link
      to="/library"
      className="inline-flex items-center gap-0.5 text-xs font-medium text-faint transition-colors duration-200 hover:text-ink"
    >
      Ver más <ChevronRightIcon size={13} />
    </Link>
  )
}

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
      <section className="mt-9">
        <div className="mb-3 flex items-center justify-between px-6">
          <h2 className="flex items-center gap-2.5 text-[1.05rem] font-semibold tracking-tight">
            {icon ?? <DiscIcon size={18} className="text-faint" />}
            {title}
          </h2>
          {action ?? <SeeMore />}
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
      className="group flex items-center gap-3 overflow-hidden rounded-2xl surface-1 px-2.5 py-2.5 transition-colors duration-200 hover:bg-white/[0.09]"
    >
      {accent ?? (
        <span className="h-14 w-14 shrink-0 overflow-hidden rounded-xl bg-surface-3">
          {cover ? (
            <img
              src={cover}
              alt=""
              loading="lazy"
              referrerPolicy="no-referrer"
              className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.05]"
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

/** Portada generada estilo Liner: degradado radial + glows difusos + glifo blanco. */
function MixCard({
  title,
  subtitle,
  gradient,
  glow,
  glyph,
  index,
  disabled,
  onPlay,
}: {
  title: string
  subtitle: string
  gradient: string
  glow: string
  glyph: ReactNode
  index: number
  disabled: boolean
  onPlay: () => void
}) {
  return (
    <motion.button
      type="button"
      {...rise(index)}
      whileHover={disabled ? undefined : { y: -4 }}
      onClick={onPlay}
      disabled={disabled}
      aria-label={`Reproducir ${title}`}
      className="group w-[150px] shrink-0 rounded-2xl text-left transition-colors duration-200 hover:bg-white/[0.03] disabled:opacity-45"
    >
      <span
        className="relative block aspect-square w-full overflow-hidden rounded-2xl shadow-[0_18px_40px_-20px_rgba(0,0,0,0.9)]"
        style={{ background: gradient }}
      >
        <span
          className="absolute -left-8 -top-10 h-32 w-32 rounded-full blur-2xl"
          style={{ background: glow, opacity: 0.75 }}
        />
        <span
          className="absolute -bottom-12 -right-8 h-36 w-36 rounded-full blur-3xl"
          style={{ background: glow, opacity: 0.5 }}
        />
        <span className="absolute inset-0 grid place-items-center text-white drop-shadow-[0_6px_18px_rgba(0,0,0,0.4)]">
          {glyph}
        </span>
        <span className="absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-black/25 to-transparent" />
        <span className="absolute bottom-2.5 right-2.5 grid h-9 w-9 place-items-center rounded-full bg-white/15 text-white opacity-0 backdrop-blur-md transition-opacity duration-200 group-hover:opacity-100">
          <PlayIcon size={15} />
        </span>
      </span>
      <span className="mt-2.5 block truncate text-sm font-semibold">{title}</span>
      <span className="mt-0.5 block truncate text-xs text-faint">{subtitle}</span>
    </motion.button>
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
  const tasteTags = [...algoGenres.slice(0, 5), ...algoArtists.slice(0, 3)]

  return (
    <div className="pb-6">
      <header className="flex items-start justify-between gap-4 px-6 pb-2 pt-7">
        <div className="min-w-0">
          <Logo size={18} className="mb-5" />
          <h1 className="text-3xl font-semibold tracking-tight">{greeting()}</h1>
          <p className="mt-1.5 text-sm text-faint">Tu música, con letras sincronizadas</p>
        </div>
        <Link
          to="/settings"
          aria-label="Ajustes"
          className="grid h-10 w-10 shrink-0 place-items-center rounded-full text-muted transition-colors duration-200 hover:bg-white/[0.06] hover:text-ink"
        >
          <SettingsIcon size={21} />
        </Link>
      </header>

      {/* Chips pequeños de gustos: acceso rápido a «Mi algoritmo». */}
      <div className="no-scrollbar mt-5 flex items-center gap-2 overflow-x-auto px-6">
        {tasteTags.map((tag, index) => (
          <motion.div key={`${tag}-${index}`} {...rise(index)} className="shrink-0">
            <Link
              to="/settings"
              className="inline-flex items-center rounded-full bg-white/[0.05] px-3 py-1.5 text-xs font-medium text-muted transition-colors duration-200 hover:bg-white/10 hover:text-ink"
            >
              {tag}
            </Link>
          </motion.div>
        ))}
        <motion.div {...rise(tasteTags.length)} className="shrink-0">
          <Link
            to="/settings"
            className="inline-flex items-center gap-1.5 rounded-full bg-white/[0.09] px-3 py-1.5 text-xs font-medium text-ink transition-colors duration-200 hover:bg-white/15"
          >
            <SparklesIcon size={13} /> {tasteTags.length > 0 ? 'Editar gustos' : 'Ajustar gustos'}
          </Link>
        </motion.div>
      </div>

      {IS_DEMO ? (
        <motion.div
          {...rise(0)}
          className="mx-6 mt-5 flex items-start gap-3 rounded-2xl surface-1 px-4 py-3"
        >
          <SparklesIcon size={16} className="mt-0.5 shrink-0 text-faint" />
          <p className="text-xs leading-relaxed text-muted">
            <span className="font-semibold text-ink">Modo demo.</span> Define{' '}
            <code className="text-ink/80">VITE_YT_API_KEY</code> para buscar e importar música real.
            La reproducción con YouTube ya funciona.
          </p>
        </motion.div>
      ) : null}

      {/* Hero de la playlist de muestra: portada grande + texto/IA, fondo de la portada. */}
      {isEmpty ? (
        <motion.section
          {...rise(1)}
          whileHover={{ y: -4 }}
          className="relative mx-6 mt-6 overflow-hidden rounded-[28px] glass-panel"
        >
          <div
            aria-hidden="true"
            className="absolute inset-0 scale-125 opacity-45 blur-[60px]"
            style={{
              backgroundImage: `url("${demoPlaylist.thumbnail}")`,
              backgroundSize: 'cover',
              backgroundPosition: 'center',
              filter: 'blur(60px) saturate(160%)',
            }}
          />
          <div
            aria-hidden="true"
            className="absolute inset-0"
            style={{
              background:
                'linear-gradient(100deg, rgba(8,8,16,0.5) 0%, rgba(8,8,16,0.74) 52%, rgba(8,8,16,0.92) 100%)',
            }}
          />
          <div className="relative flex flex-col gap-6 p-5 sm:flex-row sm:items-center sm:p-7">
            <img
              src={demoPlaylist.thumbnail}
              alt={demoPlaylist.name}
              referrerPolicy="no-referrer"
              className="h-44 w-44 shrink-0 rounded-2xl object-cover shadow-[0_28px_60px_-20px_rgba(0,0,0,0.95)] sm:h-52 sm:w-52"
            />
            <div className="min-w-0">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-2.5 py-1 text-[11px] font-medium text-ink/90 backdrop-blur-md">
                <SparklesIcon size={13} /> Selección inteligente
              </span>
              <h2 className="mt-3 text-2xl font-semibold tracking-tight">{demoPlaylist.name}</h2>
              <p className="mt-2 max-w-xl text-sm leading-relaxed text-muted">
                {IS_DEMO
                  ? demoPlaylist.description
                  : 'Una muestra para empezar. Busca en YouTube para añadir tu propia música.'}
              </p>
              <Button
                className="mt-5"
                icon={<PlayIcon size={18} />}
                onClick={() => void playQueue(demoPlaylist.tracks, 0)}
              >
                Reproducir
              </Button>
            </div>
          </div>
        </motion.section>
      ) : null}

      {/* Tus mezclas: portadas generadas con degradados y glifos. */}
      <Section title="Tus mezclas" icon={<DiscIcon size={18} className="text-faint" />}>
        <div className="no-scrollbar flex gap-3.5 overflow-x-auto px-6 pb-1">
          <MixCard
            title="Favoritos"
            subtitle={`${favorites.length} ${favorites.length === 1 ? 'canción' : 'canciones'}`}
            gradient="radial-gradient(125% 125% at 20% 12%, #a77cff 0%, #6d3ae0 42%, #2c115f 100%)"
            glow="#d9c4ff"
            glyph={<HeartFilledIcon size={46} />}
            index={0}
            disabled={favorites.length === 0}
            onPlay={() => void playQueue(favorites, 0)}
          />
          <MixCard
            title="Recientes"
            subtitle={`${history.length} ${history.length === 1 ? 'escuchada' : 'escuchadas'}`}
            gradient="radial-gradient(125% 125% at 20% 12%, #6cc0ff 0%, #2f6fe0 42%, #0b275e 100%)"
            glow="#b9dfff"
            glyph={<ClockIcon size={46} />}
            index={1}
            disabled={history.length === 0}
            onPlay={() => void playQueue(history, 0)}
          />
          <MixCard
            title="Tendencias"
            subtitle={`${demoTracks.length} selecciones`}
            gradient="radial-gradient(125% 125% at 20% 12%, #ff85b8 0%, #d83a7c 42%, #66103a 100%)"
            glow="#ffc9e0"
            glyph={<WaveformIcon size={46} />}
            index={2}
            disabled={false}
            onPlay={() => void playQueue(demoTracks, 0)}
          />
        </div>
      </Section>

      {quickTiles.length > 0 ? (
        <div className="mt-7 grid grid-cols-2 gap-2.5 px-6">
          {quickTiles.map((tile, index) => (
            <motion.div key={tile.kind === 'favorites' ? 'favorites' : tile.playlist.id} {...rise(index)} whileHover={{ y: -4 }}>
              {tile.kind === 'favorites' ? (
                <QuickTile
                  to="/library"
                  title="Tus favoritos"
                  subtitle={`${favorites.length} canciones`}
                  accent={
                    <span className="grid h-14 w-14 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-accent to-accent/50 text-[color:var(--color-accent-ink)]">
                      <HeartFilledIcon size={22} />
                    </span>
                  }
                />
              ) : (
                <QuickTile
                  to={`/playlist/${encodeURIComponent(tile.playlist.id)}`}
                  title={tile.playlist.name}
                  subtitle={`${tile.playlist.tracks.length} canciones`}
                  cover={tile.playlist.thumbnail || tile.playlist.tracks[0]?.thumbnail}
                />
              )}
            </motion.div>
          ))}
        </div>
      ) : null}

      {isEmpty ? (
        <Section
          title="Canciones de muestra"
          icon={<SparklesIcon size={18} className="text-faint" />}
        >
          <ul className="space-y-1 px-4">
            {demoTracks.slice(0, 6).map((track, index) => (
              <li key={track.id}>
                <motion.div {...rise(index)}>
                  <TrackRow track={track} queue={demoTracks} index={index} />
                </motion.div>
              </li>
            ))}
          </ul>
        </Section>
      ) : null}

      <HomeSuggestions />

      <AudiusRail />

      {algorithmReady ? (
        <SuggestionRail
          algorithm
          title="Hecho para ti"
          icon={<SparklesIcon size={18} className="text-faint" />}
          offset={1}
        />
      ) : null}

      <SuggestionRail
        title="Éxitos del momento"
        query="grandes éxitos pop del momento"
        icon={<SparklesIcon size={18} className="text-faint" />}
        offset={2}
      />

      <SuggestionRail
        title="Clásicos que nunca fallan"
        query="clásicos del rock más escuchados"
        icon={<WaveformIcon size={18} className="text-faint" />}
        offset={4}
      />

      <SuggestionRail
        title="Para descubrir"
        query="nueva música recomendada 2024"
        icon={<SparklesIcon size={18} className="text-faint" />}
        offset={6}
      />

      {history.length > 0 ? (
        <Section
          title="Escuchando ahora"
          icon={<ClockIcon size={18} className="text-faint" />}
          action={<SeeMore />}
        >
          <div className="no-scrollbar flex gap-3.5 overflow-x-auto px-6 pb-1">
            {history.slice(0, 8).map((track, index) => (
              <motion.div
                key={`${track.source}-${track.id}-${index}`}
                {...rise(index)}
                whileHover={{ y: -4 }}
                className="shrink-0 rounded-2xl transition-colors duration-200 hover:bg-white/[0.03]"
              >
                <TrackCard track={track} queue={history} index={index} />
              </motion.div>
            ))}
          </div>
        </Section>
      ) : null}

      <Section title="Tus playlists" action={<SeeMore />}>
        {playlists.length > 0 ? (
          <div className="no-scrollbar flex gap-4 overflow-x-auto px-6 pb-1">
            {playlists.map((playlist, index) => (
              <motion.div
                key={playlist.id}
                {...rise(index)}
                whileHover={{ y: -4 }}
                className="w-36 shrink-0 rounded-2xl transition-colors duration-200 hover:bg-white/[0.03]"
              >
                <PlaylistCard playlist={playlist} />
              </motion.div>
            ))}
          </div>
        ) : (
          <p className="px-6 text-sm text-faint">
            Aún no has importado playlists. Añádelas desde la Biblioteca.
          </p>
        )}
      </Section>

      {history.length > 0 ? (
        <Section title="Vuelve a escuchar" action={<SeeMore />}>
          <ul className="space-y-1 px-4">
            {history.slice(0, 6).map((track, index) => (
              <li key={`${track.source}-${track.id}-${index}`}>
                <motion.div {...rise(index)}>
                  <TrackRow track={track} queue={history} index={index} />
                </motion.div>
              </li>
            ))}
          </ul>
        </Section>
      ) : null}
    </div>
  )
}
