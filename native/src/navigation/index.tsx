import { Platform, View, Text } from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { NavigationContainer, type Theme } from '@react-navigation/native'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { HomeIcon, LibraryIcon, MicIcon, SearchIcon } from '@/components/Icons'
import { MessageToast, WallpaperLayer } from '@/components/AppChrome'
import { MiniPlayer } from '@/features/player/MiniPlayer'
import { QueueSheet } from '@/features/player/QueueSheet'
import { HomePage } from '@/pages/HomePage'
import { SearchPage } from '@/pages/SearchPage'
import { RecognizePage } from '@/pages/RecognizePage'
import { LibraryPage } from '@/pages/LibraryPage'
import { PlaylistPage } from '@/pages/PlaylistPage'
import { SettingsPage } from '@/pages/SettingsPage'
import { NowPlayingPage } from '@/pages/NowPlayingPage'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useThemeColors } from '@/shared/theme/theme'
import { TAB_LABELS, type RootStackParamList, type RootTabParamList } from './types'

const Tab = createBottomTabNavigator<RootTabParamList>()
const Stack = createNativeStackNavigator<RootStackParamList>()

const TAB_BAR_HEIGHT = 62

const TAB_ICONS: Record<keyof RootTabParamList, typeof HomeIcon> = {
  Home: HomeIcon,
  Search: SearchIcon,
  Recognize: MicIcon,
  Library: LibraryIcon,
}

function Tabs() {
  const colors = useThemeColors()
  const insets = useSafeAreaInsets()
  const hasTrack = usePlayerStore((state) => state.queue.length > 0)

  return (
    <View style={{ flex: 1 }}>
      <Tab.Navigator
        screenOptions={({ route }) => {
          const Icon = TAB_ICONS[route.name]
          return {
            headerShown: false,
            // En web el fallback de react-native-screens no oculta las pantallas
            // inactivas, así que usamos un fondo opaco solo en esa plataforma.
            sceneStyle: { backgroundColor: Platform.OS === 'web' ? colors.bg : 'transparent' },
            tabBarActiveTintColor: colors.accent,
            tabBarInactiveTintColor: colors.faint,
            tabBarStyle: {
              height: TAB_BAR_HEIGHT + insets.bottom,
              paddingBottom: insets.bottom,
              paddingTop: 8,
              backgroundColor: colors.bgElev + 'e6',
              borderTopColor: colors.hair,
            },
            tabBarLabel: ({ color }) => (
              <Text style={{ color, fontSize: 10, fontWeight: '500', marginTop: 2 }}>
                {TAB_LABELS[route.name]}
              </Text>
            ),
            tabBarIcon: ({ color, focused }) => (
              <Icon size={22} color={color} strokeWidth={focused ? 2.3 : 1.8} />
            ),
          }
        }}
      >
        <Tab.Screen name="Home" component={HomePage} />
        <Tab.Screen name="Search" component={SearchPage} />
        <Tab.Screen name="Recognize" component={RecognizePage} />
        <Tab.Screen name="Library" component={LibraryPage} />
      </Tab.Navigator>

      {hasTrack ? (
        <View
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            bottom: TAB_BAR_HEIGHT + insets.bottom + 8,
          }}
          pointerEvents="box-none"
        >
          <MiniPlayer />
        </View>
      ) : null}

      <QueueSheet />
      <MessageToast bottom={TAB_BAR_HEIGHT + insets.bottom + (hasTrack ? 84 : 12)} />
    </View>
  )
}

export function Navigation() {
  const colors = useThemeColors()

  const theme: Theme = {
    dark: colors.mode === 'dark',
    colors: {
      primary: colors.accent,
      background: 'transparent',
      card: colors.bgElev,
      text: colors.ink,
      border: colors.hair,
      notification: colors.accent,
    },
    fonts: {
      regular: { fontFamily: 'System', fontWeight: '400' },
      medium: { fontFamily: 'System', fontWeight: '500' },
      bold: { fontFamily: 'System', fontWeight: '600' },
      heavy: { fontFamily: 'System', fontWeight: '700' },
    },
  }

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <WallpaperLayer />
      <NavigationContainer theme={theme}>
        <Stack.Navigator
          screenOptions={{
            headerShown: false,
            contentStyle: { backgroundColor: Platform.OS === 'web' ? colors.bg : 'transparent' },
          }}
        >
          <Stack.Screen name="Tabs" component={Tabs} />
          <Stack.Screen name="Playlist" component={PlaylistPage} />
          <Stack.Screen name="Settings" component={SettingsPage} />
          <Stack.Screen
            name="NowPlaying"
            component={NowPlayingPage}
            options={{ presentation: 'fullScreenModal', animation: 'slide_from_bottom' }}
          />
        </Stack.Navigator>
      </NavigationContainer>
    </View>
  )
}
