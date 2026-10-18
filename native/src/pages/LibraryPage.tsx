import { useMemo, useState, type ReactNode } from 'react'
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native'
import { Artwork } from '@/components/Artwork'
import { GridIcon, HeartFilledIcon, ListIcon, PlayIcon, PlusIcon, SearchIcon } from '@/components/Icons'
import { Button, Segmented } from '@/components/ui'
import { AddToLibraryDialog } from '@/features/library/AddToLibraryDialog'
import { PlaylistCard } from '@/features/library/PlaylistCard'
import { TrackRow } from '@/features/library/TrackRow'
import { useLibraryArtists } from '@/features/library/useLibraryArtists'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useThemeColors } from '@/shared/theme/theme'

type Tab = 'lists' | 'favorites' | 'artists'
type ViewMode = 'grid' | 'list'

export function LibraryPage() {
  const colors = useThemeColors()
  const [tab, setTab] = useState<Tab>('lists')
  const [view, setView] = useState<ViewMode>('grid')
  const [query, setQuery] = useState('')
  const [addOpen, setAddOpen] = useState(false)

  const playlists = useLibraryStore((state) => state.playlists)
  const favorites = useLibraryStore((state) => state.favorites)
  const removePlaylist = useLibraryStore((state) => state.removePlaylist)
  const playQueue = usePlayerStore((state) => state.playQueue)
  const hasTrack = usePlayerStore((state) => state.queue.length > 0)

  const artists = useLibraryArtists()

  const term = query.trim().toLowerCase()
  const filteredPlaylists = useMemo(
    () => playlists.filter((playlist) => playlist.name.toLowerCase().includes(term)),
    [playlists, term],
  )
  const filteredFavorites = useMemo(
    () =>
      favorites.filter(
        (track) => track.title.toLowerCase().includes(term) || track.artist.toLowerCase().includes(term),
      ),
    [favorites, term],
  )
  const filteredArtists = useMemo(() => artists.filter((artist) => artist.name.toLowerCase().includes(term)), [artists, term])

  const placeholder =
    tab === 'lists' ? 'Buscar listas' : tab === 'favorites' ? 'Buscar canciones' : 'Buscar artistas'

  return (
    <View className="flex-1">
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 160 }}>
        <View className="px-5 pb-4 pt-6">
          <Text className="text-[1.7rem] font-semibold tracking-tight" style={{ color: colors.ink }}>
            Biblioteca
          </Text>
        </View>

        <View className="px-5">
          <Segmented
            value={tab}
            onChange={(next) => {
              setTab(next)
              setQuery('')
            }}
            tabs={[
              { id: 'lists', label: 'Listas', count: playlists.length },
              { id: 'favorites', label: 'Favoritos', count: favorites.length },
              { id: 'artists', label: 'Artistas', count: artists.length },
            ]}
          />
        </View>

        <View className="mt-3 flex-row items-center gap-2 px-5">
          <View
            className="h-10 flex-1 flex-row items-center gap-2 rounded-xl px-3.5"
            style={{ borderWidth: 1, borderColor: colors.hair, backgroundColor: colors.overlay1 }}
          >
            <SearchIcon size={17} color={colors.faint} />
            <TextInput
              value={query}
              onChangeText={setQuery}
              placeholder={placeholder}
              placeholderTextColor={colors.faint}
              className="h-10 flex-1 text-sm"
              style={{ color: colors.ink }}
            />
          </View>

          {tab === 'lists' ? (
            <View
              className="flex-row items-center gap-0.5 rounded-xl p-0.5"
              style={{ borderWidth: 1, borderColor: colors.hair, backgroundColor: colors.overlay1 }}
            >
              <Pressable
                onPress={() => setView('grid')}
                accessibilityLabel="Vista de cuadrícula"
                className="h-9 w-9 items-center justify-center rounded-lg"
                style={{ backgroundColor: view === 'grid' ? colors.accent : 'transparent' }}
              >
                <GridIcon size={18} color={view === 'grid' ? colors.accentInk : colors.faint} />
              </Pressable>
              <Pressable
                onPress={() => setView('list')}
                accessibilityLabel="Vista de lista"
                className="h-9 w-9 items-center justify-center rounded-lg"
                style={{ backgroundColor: view === 'list' ? colors.accent : 'transparent' }}
              >
                <ListIcon size={18} color={view === 'list' ? colors.accentInk : colors.faint} />
              </Pressable>
            </View>
          ) : null}
        </View>

        <View className="mt-5 px-5">
          {tab === 'lists' ? (
            filteredPlaylists.length === 0 && playlists.length === 0 ? (
              <EmptyState
                text="Aún no tienes listas."
                action={
                  <Button size="sm" icon={<PlusIcon size={16} color={colors.primaryInk} />} onPress={() => setAddOpen(true)}>
                    Crear lista
                  </Button>
                }
              />
            ) : filteredPlaylists.length === 0 ? (
              <EmptyState text="Sin resultados." />
            ) : view === 'grid' ? (
              <View className="flex-row flex-wrap gap-x-4 gap-y-6">
                <FavoritesCard count={favorites.length} onOpen={() => setTab('favorites')} />
                {filteredPlaylists.map((playlist) => (
                  <PlaylistCard key={playlist.id} playlist={playlist} size={150} />
                ))}
              </View>
            ) : (
              <View className="gap-1.5">
                <FavoritesRow count={favorites.length} onOpen={() => setTab('favorites')} />
                {filteredPlaylists.map((playlist) => (
                  <View key={playlist.id} className="flex-row items-center gap-2">
                    <View className="min-w-0 flex-1">
                      <PlaylistCard playlist={playlist} variant="wide" />
                    </View>
                    <Pressable
                      accessibilityLabel={`Eliminar ${playlist.name}`}
                      onPress={() => removePlaylist(playlist.id)}
                      className="h-9 w-9 items-center justify-center rounded-full"
                    >
                      <Text className="text-lg leading-none" style={{ color: colors.faint }}>
                        ×
                      </Text>
                    </Pressable>
                  </View>
                ))}
              </View>
            )
          ) : null}

          {tab === 'favorites' ? (
            filteredFavorites.length > 0 ? (
              <>
                <View className="mb-3 flex-row items-center justify-between">
                  <Text className="text-xs" style={{ color: colors.faint }}>
                    {filteredFavorites.length} {filteredFavorites.length === 1 ? 'canción' : 'canciones'}
                  </Text>
                  <Button
                    size="sm"
                    variant="subtle"
                    icon={<PlayIcon size={16} color={colors.ink} />}
                    onPress={() => void playQueue(filteredFavorites, 0)}
                  >
                    Reproducir
                  </Button>
                </View>
                <View className="-mx-2">
                  {filteredFavorites.map((track, index) => (
                    <TrackRow
                      key={`${track.source}-${track.id}-${index}`}
                      track={track}
                      queue={filteredFavorites}
                      index={index}
                    />
                  ))}
                </View>
              </>
            ) : (
              <EmptyState
                text={
                  favorites.length === 0
                    ? 'Toca el corazón en cualquier canción para guardarla aquí.'
                    : 'Sin resultados.'
                }
              />
            )
          ) : null}

          {tab === 'artists' ? (
            filteredArtists.length > 0 ? (
              <View className="gap-1">
                {filteredArtists.map((artist) => (
                  <Pressable
                    key={artist.name}
                    onPress={() => void playQueue(artist.tracks, 0)}
                    className="flex-row items-center gap-3.5 rounded-2xl px-1 py-2"
                  >
                    <Artwork src={artist.tracks[0]?.thumbnail} style={{ width: 48, height: 48 }} radius={999} iconSize={22} />
                    <View className="min-w-0 flex-1">
                      <Text numberOfLines={1} className="text-[0.95rem] font-medium" style={{ color: colors.ink }}>
                        {artist.name}
                      </Text>
                      <Text numberOfLines={1} className="mt-0.5 text-xs" style={{ color: colors.faint }}>
                        {artist.tracks.length} {artist.tracks.length === 1 ? 'canción' : 'canciones'}
                      </Text>
                    </View>
                    <PlayIcon size={18} color={colors.faint} />
                  </Pressable>
                ))}
              </View>
            ) : (
              <EmptyState
                text={artists.length === 0 ? 'Escucha o guarda música para ver tus artistas.' : 'Sin resultados.'}
              />
            )
          ) : null}
        </View>
      </ScrollView>

      <Pressable
        accessibilityLabel="Crear o importar lista"
        onPress={() => setAddOpen(true)}
        className="absolute right-5 h-14 w-14 items-center justify-center rounded-full"
        style={{ bottom: hasTrack ? 150 : 90, backgroundColor: colors.ink }}
      >
        <PlusIcon size={26} strokeWidth={2.4} color={colors.bg} />
      </Pressable>

      <AddToLibraryDialog open={addOpen} onClose={() => setAddOpen(false)} />
    </View>
  )
}

function EmptyState({ text, action }: { text: string; action?: ReactNode }) {
  const colors = useThemeColors()
  return (
    <View className="items-center gap-3 py-12">
      <Text className="max-w-[16rem] text-center text-sm" style={{ color: colors.faint }}>
        {text}
      </Text>
      {action}
    </View>
  )
}

function FavoritesCard({ count, onOpen }: { count: number; onOpen: () => void }) {
  const colors = useThemeColors()
  return (
    <Pressable onPress={onOpen} style={{ width: 150 }}>
      <View
        className="items-center justify-center rounded-2xl"
        style={{
          width: 150,
          height: 150,
          backgroundColor: colors.accent + '26',
          borderWidth: 1,
          borderColor: colors.hair,
        }}
      >
        <HeartFilledIcon size={46} color={colors.accent} />
      </View>
      <Text numberOfLines={1} className="mt-2.5 text-sm font-semibold" style={{ color: colors.ink }}>
        Me gusta
      </Text>
      <Text numberOfLines={1} className="text-xs" style={{ color: colors.faint }}>
        {count} {count === 1 ? 'canción' : 'canciones'}
      </Text>
    </Pressable>
  )
}

function FavoritesRow({ count, onOpen }: { count: number; onOpen: () => void }) {
  const colors = useThemeColors()
  return (
    <Pressable
      onPress={onOpen}
      className="flex-row items-center gap-3 rounded-2xl p-2"
      style={{ backgroundColor: colors.overlay1 }}
    >
      <View
        className="h-14 w-14 items-center justify-center rounded-xl"
        style={{ backgroundColor: colors.accent + '26' }}
      >
        <HeartFilledIcon size={24} color={colors.accent} />
      </View>
      <View className="min-w-0 flex-1">
        <Text numberOfLines={1} className="font-medium" style={{ color: colors.ink }}>
          Me gusta
        </Text>
        <Text className="mt-0.5 text-xs" style={{ color: colors.faint }}>
          {count} {count === 1 ? 'canción' : 'canciones'}
        </Text>
      </View>
    </Pressable>
  )
}
