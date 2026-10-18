export interface ArtworkColors {
  primary: string
  secondary: string
  palette: string[]
}

/** Paleta por defecto (tema Liner) cuando no se puede analizar la portada. */
export const DEFAULT_COLORS: ArtworkColors = {
  primary: 'hsl(252 70% 62%)',
  secondary: 'hsl(200 70% 55%)',
  palette: ['hsl(252 70% 62%)', 'hsl(200 70% 55%)', 'hsl(300 60% 58%)'],
}

function rgbToHsl(r: number, g: number, b: number): [number, number, number] {
  const red = r / 255
  const green = g / 255
  const blue = b / 255
  const max = Math.max(red, green, blue)
  const min = Math.min(red, green, blue)
  const delta = max - min
  let hue = 0

  if (delta !== 0) {
    if (max === red) hue = ((green - blue) / delta) % 6
    else if (max === green) hue = (blue - red) / delta + 2
    else hue = (red - green) / delta + 4
    hue *= 60
    if (hue < 0) hue += 360
  }

  const lightness = (max + min) / 2
  const saturation = delta === 0 ? 0 : delta / (1 - Math.abs(2 * lightness - 1))
  return [hue, saturation, lightness]
}

interface Bucket {
  count: number
  r: number
  g: number
  b: number
  saturation: number
}

function computePalette(data: Uint8ClampedArray): ArtworkColors {
  const buckets = new Map<number, Bucket>()

  for (let i = 0; i < data.length; i += 4) {
    const r = data[i]
    const g = data[i + 1]
    const b = data[i + 2]
    const a = data[i + 3]
    if (a < 128) continue

    const [, sat, light] = rgbToHsl(r, g, b)
    if (light < 0.08 || light > 0.94) continue
    if (sat < 0.12) continue

    const key = ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4)
    const bucket = buckets.get(key) ?? { count: 0, r: 0, g: 0, b: 0, saturation: sat }
    bucket.count += 1
    bucket.r += r
    bucket.g += g
    bucket.b += b
    bucket.saturation += sat
    buckets.set(key, bucket)
  }

  const ranked = [...buckets.values()]
    .map((bucket) => ({
      count: bucket.count,
      r: bucket.r / bucket.count,
      g: bucket.g / bucket.count,
      b: bucket.b / bucket.count,
    }))
    .sort((a, b) => b.count - a.count)

  if (ranked.length === 0) return DEFAULT_COLORS

  const toCss = (rgb: { r: number; g: number; b: number }): string => {
    const [hue, sat, light] = rgbToHsl(rgb.r, rgb.g, rgb.b)
    const boostedSat = Math.round(Math.min(88, Math.max(45, sat * 100 + 18)))
    const boostedLight = Math.round(Math.min(68, Math.max(46, light * 100 + 8)))
    return `hsl(${Math.round(hue)} ${boostedSat}% ${boostedLight}%)`
  }

  const first = ranked[0]
  const distinct =
    ranked.find((candidate) => {
      const [h1] = rgbToHsl(first.r, first.g, first.b)
      const [h2] = rgbToHsl(candidate.r, candidate.g, candidate.b)
      const diff = Math.abs(h1 - h2)
      return Math.min(diff, 360 - diff) > 40
    }) ?? ranked[Math.min(1, ranked.length - 1)]

  const palette = ranked.slice(0, 3).map(toCss)

  return {
    primary: toCss(first),
    secondary: toCss(distinct),
    palette: palette.length >= 2 ? palette : [toCss(first), toCss(distinct), toCss(first)],
  }
}

/** Extrae colores dominantes de una imagen remota mediante canvas. */
export function extractColorsFromImage(url: string): Promise<ArtworkColors> {
  return new Promise((resolve, reject) => {
    if (!url) {
      reject(new Error('empty-url'))
      return
    }
    const image = new Image()
    image.crossOrigin = 'anonymous'
    image.referrerPolicy = 'no-referrer'
    image.onload = () => {
      try {
        const size = 48
        const canvas = document.createElement('canvas')
        canvas.width = size
        canvas.height = size
        const context = canvas.getContext('2d', { willReadFrequently: true })
        if (!context) {
          reject(new Error('no-context'))
          return
        }
        context.drawImage(image, 0, 0, size, size)
        const pixels = context.getImageData(0, 0, size, size).data
        resolve(computePalette(pixels))
      } catch (error) {
        reject(error)
      }
    }
    image.onerror = () => reject(new Error('image-load-failed'))
    image.src = url
  })
}
