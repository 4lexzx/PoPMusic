import { useState, type CSSProperties, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Artwork } from '@/components/Artwork'
import {
  ChevronRightIcon,
  ClockIcon,
  DiscIcon,
  HeartFilledIcon,
  MoreIcon,
  PauseIcon,
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
import { TrackMenu, TrackRow } from '@/features/library/TrackRow'
import { IS_DEMO } from '@/shared/config'
import { demoPlaylist, demoTracks } from '@/shared/data/demo'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useSettingsStore } from '@/shared/store/settingsStore'
import type { Track } from '@/shared/types/track'

/** Movimiento calmado: fundido corto + micro-desplazamiento. */
const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1]

function rise(index: number) {
  return {
    initial: { opacity: 0, y: 6 },
    animate: { opacity: 1, y: 0 },
    transition: { duration: 0.28, delay: index * 0.04, ease: EASE },
  }
}

/** Paleta cálida de la referencia: fondo casi negro, superficies y texto. */
const warmVars = {
  '--color-ink': '#f5f2ec',
  '--color-muted': '#b6b0a6',
  '--color-faint': '#8c8780',
} as CSSProperties

/** Compensa el padding de `.desktop-page` para conseguir 36 px reales y fondo a sangre. */
const warmShell: CSSProperties = {
  ...warmVars,
  margin: '-22px -34px -40px',
  padding: '22px 36px 40px',
  background: '#0c0b0a',
  minHeight: '100%',
}

function uniqueTracks(tracks: Track[]): Track[] {
  const seen = new Set<string>()
  return tracks.filter((track) => {
    const key = `${track.source}:${track.id}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

/** Enlace «Ver más» compartido por las cabeceras de sección. */
function SeeMore() {
  return (
    <Link
      to="/library"
      className="inline-flex items-center gap-0.5 text-xs font-medium text-[#8c8780] transition-colors duration-200 hover:text-[#f5f2ec]"
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

/** Círculo blanco de 22 px con el glifo en negro que encabeza cada sección. */
function SectionGlyph({ children }: { children: ReactNode }) {
  return (
    <span className="grid h-[22px] w-[22px] shrink-0 place-items-center rounded-full bg-[#f5f2ec] text-[#0c0b0a]">
      {children}
    </span>
  )
}

/** Glifo de llama (sección «Escuchando ahora mismo»). */
function FlameGlyph() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12.8 2c.4 2.5-.9 3.9-2.2 5.2-1.4 1.4-2.9 2.9-2.9 5.8a4.4 4.4 0 0 0 8.8 0c0-1.6-.7-2.9-1.6-3.9.1 1.2-.4 2-1.2 2.4.4-2.6-.4-6.5-.8-9.5Z" />
    </svg>
  )
}

function Section({
  title,
  icon,
  glyph,
  action,
  children,
}: {
  title: string
  icon?: ReactNode
  glyph?: ReactNode
  action?: ReactNode
  children: ReactNode
}) {
  return (
    <Reveal>
      <section className="mt-9">
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="flex items-center gap-2.5 text-[20px] font-bold tracking-[-0.01em] text-[#f5f2ec]">
            <SectionGlyph>{glyph ?? icon ?? <DiscIcon size={13} />}</SectionGlyph>
            {title}
          </h2>
          {action === undefined ? <SeeMore /> : action}
        </div>
        {children}
      </section>
    </Reveal>
  )
}

interface QuickTileProps {
  to?: string
  title: string
  subtitle: string
  cover?: string
  accent?: ReactNode
  onClick?: () => void
  onMore?: () => void
}

/** Tarjeta ancha de acceso rápido: miniatura, texto y botón ⋮. */
function QuickTile({ to, title, subtitle, cover, accent, onClick, onMore }: QuickTileProps) {
  const content = (
    <>
      {accent ?? (
        <span className="h-10 w-10 shrink-0 overflow-hidden rounded-lg bg-[#262421]">
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
        <span className="block truncate text-[14px] font-semibold leading-tight text-[#f5f2ec]">
          {title}
        </span>
        <span className="mt-0.5 block truncate text-[12px] text-[#8c8780]">{subtitle}</span>
      </span>
    </>
  )

  return (
    <div className="flex h-[58px] items-center gap-3 rounded-[12px] border border-white/[0.06] bg-[#1a1917] py-1 pl-2.5 pr-1.5 transition-colors duration-200 hover:bg-[#211f1c]">
      {to ? (
        <Link to={to} onClick={onClick} className="flex min-w-0 flex-1 items-center gap-3">
          {content}
        </Link>
      ) : (
        <button
          type="button"
          onClick={onClick}
          className="flex min-w-0 flex-1 items-center gap-3 text-left"
        >
          {content}
        </button>
      )}
      <button
        type="button"
        aria-label="Más opciones"
        onClick={onMore}
        className="grid h-8 w-8 shrink-0 place-items-center rounded-lg text-[#8c8780] transition-colors duration-200 hover:bg-white/[0.06] hover:text-[#f5f2ec]"
      >
        <MoreIcon size={17} />
      </button>
    </div>
  )
}

/** Tarjeta de álbum cuadrada (~175 px) usada en las filas horizontales. */
function AlbumCard({ track, queue, index }: { track: Track; queue: Track[]; index: number }) {
  const playTrack = usePlayerStore((state) => state.playTrack)
  const toggle = usePlayerStore((state) => state.toggle)
  const current = usePlayerStore((state) => state.queue[state.currentIndex] ?? null)
  const isPlaying = usePlayerStore((state) => state.isPlaying)

  const isCurrent = current ? sameTrack(current, track) : false
  const active = isCurrent && isPlaying

  return (
    <motion.div {...rise(index)} whileHover={{ y: -2 }} className="w-[175px] shrink-0">
      <button
        type="button"
        onClick={() => (isCurrent ? toggle() : void playTrack(track, queue))}
        aria-label={active ? `Pausar ${track.title}` : `Reproducir ${track.title}`}
        className="group block w-full text-left"
      >
        <span className="relative block">
          <Artwork
            src={track.thumbnail}
            alt={track.title}
            className="aspect-square w-full"
            rounded="rounded-[10px]"
            iconSize={34}
          />
          <span className="absolute inset-x-0 bottom-0 h-1/2 rounded-b-[10px] bg-gradient-to-t from-black/45 to-transparent" />
          <span
            className={`absolute bottom-2 right-2 grid h-9 w-9 place-items-center rounded-full bg-[#f5f2ec] text-[#0c0b0a] shadow-lg transition-opacity duration-200 group-hover:opacity-100 max-md:opacity-100 ${
              isCurrent ? 'opacity-100' : 'opacity-0'
            }`}
          >
            {active ? <PauseIcon size={15} /> : <PlayIcon size={15} />}
          </span>
        </span>
        <span className="mt-2 block truncate text-[14px] font-semibold text-[#f5f2ec]">
          {track.title}
        </span>
        <span className="mt-0.5 block truncate text-[12px] text-[#8c8780]">{track.artist}</span>
      </button>
    </motion.div>
  )
}

function AlbumRail({ items }: { items: Track[] }) {
  return (
    <div className="no-scrollbar flex gap-3.5 overflow-x-auto pb-2">
      {items.map((track, index) => (
        <AlbumCard
          key={`${track.source}-${track.id}-${index}`}
          track={track}
          queue={items}
          index={index}
        />
      ))}
    </div>
  )
}

export function HomePage() {
  const navigate = useNavigate()
  const playlists = useLibraryStore((state) => state.playlists)
  const history = useLibraryStore((state) => state.history)
  const favorites = useLibraryStore((state) => state.favorites)
  const playQueue = usePlayerStore((state) => state.playQueue)
  const algoGenres = useSettingsStore((state) => state.genres)
  const algoArtists = useSettingsStore((state) => state.artists)
  const algorithmReady = algoGenres.length > 0 || algoArtists.length > 0

  const [menuTrack, setMenuTrack] = useState<{ track: Track; queue: Track[] } | null>(null)

  const isEmpty = playlists.length === 0 && history.length === 0 && favorites.length === 0

  // Las 6 tarjetas de acceso rápido: favoritos + 5 pistas reales del store.
  const quickTracks = uniqueTracks([...history, ...favorites, ...demoTracks]).slice(0, 5)
  // Filas de álbumes: "escuchando ahora" y "vuelve a escuchar".
  const nowListening = uniqueTracks([...favorites, ...history, ...demoTracks]).slice(0, 10)
  const replay = uniqueTracks([...history, ...demoTracks]).slice(0, 10)

  const tasteTags = [...algoGenres.slice(0, 5), ...algoArtists.slice(0, 3)]

  return (
    <div className="pb-6" style={warmShell}>
      <header className="flex items-start justify-between gap-4 pb-2 pt-1">
        <div className="min-w-0">
          <Logo size={16} className="mb-4" />
          <h1 className="text-[28px] font-bold tracking-tight text-[#f5f2ec]">{greeting()}</h1>
          <p className="mt-1 text-[13px] text-[#8c8780]">Tu música, con letras sincronizadas</p>
        </div>
        <Link
          to="/settings"
          aria-label="Ajustes"
          className="grid h-10 w-10 shrink-0 place-items-center rounded-full text-[#8c8780] transition-colors duration-200 hover:bg-white/[0.06] hover:text-[#f5f2ec]"
        >
          <SettingsIcon size={21} />
        </Link>
      </header>

      {/* 1 · Acceso rápido — rejilla 3×2 de tarjetas anchas. */}
      <Section title="Acceso rápido" glyph={<SparklesIcon size={13} />} action={false}>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <motion.div {...rise(0)} whileHover={{ y: -2 }}>
            <QuickTile
              title="Canciones que te gustan"
              subtitle={`${favorites.length} ${favorites.length === 1 ? 'canción' : 'canciones'}`}
              accent={
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-[#262421] text-[#f5f2ec]">
                  <HeartFilledIcon size={18} />
                </span>
              }
              onClick={() =>
                favorites.length > 0 ? void playQueue(favorites, 0) : navigate('/library')
              }
              onMore={() => navigate('/library')}
            />
          </motion.div>

          {quickTracks.map((track, index) => (
            <motion.div key={`${track.source}-${track.id}`} {...rise(index + 1)} whileHover={{ y: -2 }}>
              <QuickTile
                title={track.title}
                subtitle={track.artist}
                cover={track.thumbnail}
                onClick={() => void playQueue(quickTracks, index)}
                onMore={() => setMenuTrack({ track, queue: quickTracks })}
              />
            </motion.div>
          ))}
        </div>
      </Section>

      {/* 2 · Escuchando ahora mismo — fila de álbumes cuadrados. */}
      <Section title="Escuchando ahora mismo" glyph={<FlameGlyph />} action={false}>
        <AlbumRail items={nowListening} />
      </Section>

      {/* 3 · Vuelve a escuchar — historial (o Audius si está vacío). */}
      <Section title="Vuelve a escuchar" glyph={<ClockIcon size={13} />} action={false}>
        <AlbumRail items={replay} />
      </Section>
      {history.length === 0 ? <AudiusRail title="Descubre en segundo plano" /> : null}

      {/* Modo demo: sigue avisando de la configuración pendiente. */}
      {IS_DEMO ? (
        <motion.div
          {...rise(0)}
          className="mt-8 flex items-start gap-3 rounded-[12px] border border-white/[0.06] bg-[#1a1917] px-4 py-3"
        >
          <SparklesIcon size={16} className="mt-0.5 shrink-0 text-[#8c8780]" />
          <p className="text-xs leading-relaxed text-[#b6b0a6]">
            <span className="font-semibold text-[#f5f2ec]">Modo demo.</span> Define{' '}
            <code className="text-[#f5f2ec]/80">VITE_YT_API_KEY</code> para buscar e importar música
            real. La reproducción con YouTube ya funciona.
          </p>
        </motion.div>
      ) : null}

      {/* Hero de la playlist de muestra (solo con biblioteca vacía), sin degradados. */}
      {isEmpty ? (
        <motion.section
          {...rise(1)}
          whileHover={{ y: -2 }}
          className="relative mt-8 overflow-hidden rounded-[20px] border border-white/[0.06] bg-[#1a1917]"
        >
          <div className="relative flex flex-col gap-5 p-5 sm:flex-row sm:items-center">
            <img
              src={demoPlaylist.thumbnail}
              alt={demoPlaylist.name}
              referrerPolicy="no-referrer"
              className="h-32 w-32 shrink-0 rounded-xl object-cover shadow-[0_18px_40px_-18px_rgba(0,0,0,0.9)]"
            />
            <div className="min-w-0">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-white/[0.08] px-2.5 py-1 text-[11px] font-medium text-[#f5f2ec]">
                <SparklesIcon size={13} /> Selección inteligente
              </span>
              <h2 className="mt-3 text-xl font-semibold tracking-tight text-[#f5f2ec]">
                {demoPlaylist.name}
              </h2>
              <p className="mt-1.5 max-w-xl text-[13px] leading-relaxed text-[#b6b0a6]">
                {IS_DEMO
                  ? demoPlaylist.description
                  : 'Una muestra para empezar. Busca en YouTube para añadir tu propia música.'}
              </p>
              <Button
                className="mt-4"
                icon={<PlayIcon size={18} />}
                onClick={() => void playQueue(demoPlaylist.tracks, 0)}
              >
                Reproducir
              </Button>
            </div>
          </div>
        </motion.section>
      ) : null}

      {/* Resto de descubrimientos y biblioteca, accesibles al desplazarse. */}
      {isEmpty ? (
        <Section title="Canciones de muestra" glyph={<SparklesIcon size={13} />}>
          <ul className="space-y-1">
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

      {history.length > 0 ? <AudiusRail title="Suena en segundo plano" /> : null}

      {algorithmReady ? (
        <SuggestionRail
          algorithm
          title="Hecho para ti"
          icon={<SparklesIcon size={18} className="text-[#f5f2ec]" />}
          offset={1}
        />
      ) : null}

      <SuggestionRail
        title="Éxitos del momento"
        query="grandes éxitos pop del momento"
        icon={<SparklesIcon size={18} className="text-[#f5f2ec]" />}
        offset={2}
      />

      <SuggestionRail
        title="Clásicos que nunca fallan"
        query="clásicos del rock más escuchados"
        icon={<WaveformIcon size={18} className="text-[#8c8780]" />}
        offset={4}
      />

      <SuggestionRail
        title="Para descubrir"
        query="nueva música recomendada 2024"
        icon={<SparklesIcon size={18} className="text-[#f5f2ec]" />}
        offset={6}
      />

      <Section title="Tus playlists">
        {playlists.length > 0 ? (
          <div className="no-scrollbar flex gap-4 overflow-x-auto pb-1">
            {playlists.map((playlist, index) => (
              <motion.div
                key={playlist.id}
                {...rise(index)}
                whileHover={{ y: -2 }}
                className="w-36 shrink-0"
              >
                <PlaylistCard playlist={playlist} />
              </motion.div>
            ))}
          </div>
        ) : (
          <p className="text-[13px] text-[#8c8780]">
            Aún no has importado playlists. Añádelas desde la Biblioteca.
          </p>
        )}
      </Section>

      {/* Gustos: movidos al final, en pequeño, como pide la referencia. */}
      <div className="mt-10 flex flex-wrap items-center gap-2 border-t border-white/[0.06] pt-5">
        <span className="mr-1 text-[11px] font-medium uppercase tracking-wide text-[#8c8780]">
          Tus gustos
        </span>
        {tasteTags.map((tag, index) => (
          <Link
            key={`${tag}-${index}`}
            to="/settings"
            className="rounded-full border border-white/[0.06] bg-[#1a1917] px-2.5 py-1 text-[11px] font-medium text-[#b6b0a6] transition-colors duration-200 hover:text-[#f5f2ec]"
          >
            {tag}
          </Link>
        ))}
        <Link
          to="/settings"
          className="inline-flex items-center gap-1 rounded-full border border-white/[0.06] bg-[#1a1917] px-2.5 py-1 text-[11px] font-medium text-[#f5f2ec] transition-colors duration-200 hover:bg-[#211f1c]"
        >
          <SparklesIcon size={12} /> {tasteTags.length > 0 ? 'Editar gustos' : 'Ajustar gustos'}
        </Link>
      </div>

      {/* Menú ⋮ de las pistas de acceso rápido. */}
      {menuTrack ? (
        <TrackMenu
          track={menuTrack.track}
          queue={menuTrack.queue}
          onClose={() => setMenuTrack(null)}
        />
      ) : null}
    </div>
  )
}
