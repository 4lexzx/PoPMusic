import { TrackRow } from '@/features/library/TrackRow'
import type { Track } from '@/shared/types/track'

interface SearchResultsProps {
  results: Track[]
}

/** Lista de resultados de búsqueda reutilizable. */
export function SearchResults({ results }: SearchResultsProps) {
  return (
    <ul className="space-y-0.5 px-2">
      {results.map((track, index) => (
        <li key={`${track.source}-${track.id}-${index}`}>
          <TrackRow track={track} queue={results} index={index} />
        </li>
      ))}
    </ul>
  )
}
