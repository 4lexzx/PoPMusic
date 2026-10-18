import { View } from 'react-native'
import { TrackRow } from '@/features/library/TrackRow'
import type { Track } from '@/shared/types/track'

interface SearchResultsProps {
  results: Track[]
}

/** Lista de resultados de búsqueda reutilizable. */
export function SearchResults({ results }: SearchResultsProps) {
  return (
    <View className="px-2">
      {results.map((track, index) => (
        <TrackRow key={`${track.source}-${track.id}-${index}`} track={track} queue={results} index={index} />
      ))}
    </View>
  )
}
