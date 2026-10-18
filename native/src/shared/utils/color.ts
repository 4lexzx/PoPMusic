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

/**
 * En la web se extraían los colores dominantes con canvas. En React Native no
 * existe canvas, así que se devuelve la paleta por defecto (el fondo dinámico
 * usa el acento configurado por el usuario).
 */
export function extractColorsFromImage(url: string): Promise<ArtworkColors> {
  if (!url || typeof url !== 'string') {
    return Promise.reject(new Error('empty-url'))
  }
  return Promise.resolve(DEFAULT_COLORS)
}
