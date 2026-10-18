import { useEffect } from 'react'
import { Pressable, StyleSheet, Text, View } from 'react-native'
import { Image } from 'expo-image'
import { LinearGradient } from 'expo-linear-gradient'
import { findWallpaper, useSettingsStore } from '@/shared/store/settingsStore'
import { useUiStore } from '@/shared/store/uiStore'
import { useThemeColors } from '@/shared/theme/theme'

/** Capa de fondo de pantalla (degradado o imagen) tras la interfaz. */
export function WallpaperLayer() {
  const wallpaper = useSettingsStore((state) => state.wallpaper)
  const wallpaperImage = useSettingsStore((state) => state.wallpaperImage)
  const opacity = useSettingsStore((state) => state.wallpaperOpacity)
  const blur = useSettingsStore((state) => state.wallpaperBlur)
  const dim = useSettingsStore((state) => state.wallpaperDim)

  const preset = findWallpaper(wallpaper)

  if (!preset && !wallpaperImage) return null

  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      {wallpaperImage ? (
        <Image
          source={{ uri: wallpaperImage }}
          style={[StyleSheet.absoluteFill, { opacity }]}
          contentFit="cover"
          blurRadius={blur}
        />
      ) : preset ? (
        <LinearGradient
          colors={preset.colors as [string, string, ...string[]]}
          style={[StyleSheet.absoluteFill, { opacity }]}
        />
      ) : null}
      <View style={[StyleSheet.absoluteFill, { backgroundColor: `rgba(0,0,0,${dim})` }]} />
    </View>
  )
}

/** Aviso flotante (toast) con autocierre. */
export function MessageToast({ bottom = 96 }: { bottom?: number }) {
  const colors = useThemeColors()
  const message = useUiStore((state) => state.message)
  const dismiss = useUiStore((state) => state.dismissMessage)

  useEffect(() => {
    if (!message) return
    const timer = setTimeout(dismiss, 4500)
    return () => clearTimeout(timer)
  }, [message, dismiss])

  if (!message) return null

  return (
    <View
      pointerEvents="box-none"
      className="absolute inset-x-0 z-50 items-center px-4"
      style={{ bottom }}
    >
      <Pressable
        onPress={dismiss}
        className="max-w-md rounded-2xl px-4 py-3"
        style={{
          backgroundColor: message.tone === 'error' ? colors.accent : colors.bgElev,
          borderWidth: message.tone === 'error' ? 0 : 1,
          borderColor: colors.hair,
        }}
      >
        <Text
          className="text-sm font-medium"
          style={{ color: message.tone === 'error' ? '#ffffff' : colors.ink }}
        >
          {message.text}
        </Text>
      </Pressable>
    </View>
  )
}
