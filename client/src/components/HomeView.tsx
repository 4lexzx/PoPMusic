import { Play, Pause, Heart, Radio, Music2 } from 'lucide-react'
import type { Track } from '../types'

interface Props {
  tracks: Track[]
  current: Track | null
  playing: boolean
  onPlay: (track: Track, all: Track[]) => void
  isFavorite: (id: string) => boolean
  onFavorite: (track: Track) => void
}

export default function HomeView({ tracks, current, playing, onPlay, isFavorite, onFavorite }: Props) {
  const quick = tracks.slice(0, 10)
  const shelf = tracks.slice(10)

  return (
    <div className="homeView">
      {quick.length > 0 && (
        <>
          <div className="sectTitle">
            <Radio size={20} />
            <h1>Escuchando ahora mismo</h1>
          </div>
          <div className="quickPanel">
            {quick.map((track) => {
              const active = current?.id === track.id
              return (
                <div
                  key={track.id}
                  className={`quickRow${active ? ' active' : ''}`}
                  onDoubleClick={() => onPlay(track, tracks)}
                  role="button"
                  tabIndex={0}
                  onClick={() => onPlay(track, tracks)}
                >
                  <img className="quickArt" src={track.coverUrl} alt="" loading="lazy" />
                  <div className="quickMeta">
                    <strong>{track.title}</strong>
                    <span>{track.artists}</span>
                  </div>
                  <button
                    className="quickPlay"
                    onClick={(e) => { e.stopPropagation(); onPlay(track, tracks) }}
                    title={active && playing ? 'Pausar' : 'Reproducir'}
                  >
                    {active && playing
                      ? <Pause size={13} fill="currentColor" />
                      : <Play size={13} fill="currentColor" />}
                  </button>
                </div>
              )
            })}
          </div>
        </>
      )}

      {shelf.length > 0 && (
        <div className="shelf">
          <div className="sectTitle">
            <Music2 size={20} />
            <h1>Más populares</h1>
          </div>
          <div className="shelfRow">
            {shelf.map((track) => {
              const active = current?.id === track.id
              return (
                <div key={track.id} className={`shelfCard${active ? ' active' : ''}`}>
                  <div className="shelfArtWrap">
                    <img src={track.coverUrl} alt="" loading="lazy" onClick={() => onPlay(track, tracks)} />
                    <button
                      className={`shelfFav${isFavorite(track.id) ? ' on' : ''}`}
                      onClick={() => onFavorite(track)}
                      title="Favorito"
                    >
                      <Heart size={14} fill={isFavorite(track.id) ? 'currentColor' : 'none'} />
                    </button>
                    <button className="shelfPlay" onClick={() => onPlay(track, tracks)} title="Reproducir">
                      {active && playing
                        ? <Pause size={17} fill="currentColor" />
                        : <Play size={17} fill="currentColor" />}
                    </button>
                  </div>
                  <strong>{track.title}</strong>
                  <span>{track.artists}</span>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {tracks.length === 0 && (
        <div className="empty">
          <Music2 size={32} />
          <span>Cargando el catálogo…</span>
        </div>
      )}
    </div>
  )
}
