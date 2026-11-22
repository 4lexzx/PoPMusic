/**
 * Lee un archivo de imagen y devuelve un data URL reducido (máx. 1600px)
 * para no saturar localStorage al guardarlo como fondo de pantalla.
 */
export function fileToDownscaledDataUrl(file: File, maxSize = 1600): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('No se pudo leer la imagen.'))
    reader.onload = () => {
      const image = new Image()
      image.onerror = () => reject(new Error('Imagen no válida.'))
      image.onload = () => {
        const scale = Math.min(1, maxSize / Math.max(image.width, image.height))
        const width = Math.max(1, Math.round(image.width * scale))
        const height = Math.max(1, Math.round(image.height * scale))
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const context = canvas.getContext('2d')
        if (!context) {
          reject(new Error('No se pudo procesar la imagen.'))
          return
        }
        context.drawImage(image, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', 0.85))
      }
      image.src = reader.result as string
    }
    reader.readAsDataURL(file)
  })
}
