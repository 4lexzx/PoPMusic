import { Play, Pause, Heart, Music2 } from 'lucide-react'
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
  const featured = tracks[0]
  const rest = tracks.slice(1)

  return (
    <div className="homeView">
      {featured && (
        <div className="hero">
          <img className="heroArt" src={featured.coverUrl} alt="" />
          <div className="heroShade" />
          <div className="heroText">
            <span className="heroTag"><span className="dot" /> CANCIÓN DESTACADA</span>
            <h1>{featured.title}</h1>
            <p>{featured.artists}</p>
            <div className="heroActions">
              <button className="heroPlay" onClick={() => onPlay(featured, tracks)}>
                {current?.id === featured.id && playing ? <Pause size={20} fill="currentColor" /> : <Play size={20} fill="currentColor" />}
                Reproducir
              </button>
              <button className={`heroFav ${isFavorite(featured.id) ? 'on' : ''}`} onClick={() => onFavorite(featured)}>
                <Heart size={19} fill={isFavorite(featured.id) ? 'currentColor' : 'none'} />
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="sectionHead">
        <h1>Top canciones</h1>
        <span>Lo más escuchado ahora</span>
      </div>

      <div className="trackGrid">
        {rest.map((track) => {
          const active = current?.id === track.id
          return (
            <div key={track.id} className={`trackCard${active ? ' active' : ''}`}>
              <div className="cardArtWrap">
                <img src={track.coverUrl} alt="" loading="lazy" />
                <button className="cardPlay" onClick={() => onPlay(track, tracks)}>
                  {active && playing ? <Pause size={19} fill="currentColor" /> : <Play size={19} fill="currentColor" />}
                </button>
                <button
                  className={`cardFav ${isFavorite(track.id) ? 'on' : ''}`}
                  onClick={() => onFavorite(track)}
                >
                  <Heart size={16} fill={isFavorite(track.id) ? 'currentColor' : 'none'} />
                </button>
              </div>
              <strong>{track.title}</strong>
              <span>{track.artists}</span>
            </div>
          )
        })}
        {tracks.length === 0 && (
          <div className="empty"><Music2 size={36} /><span>Cargando el catálogo…</span></div>
        )}
      </div>
    </div>
  )
}
