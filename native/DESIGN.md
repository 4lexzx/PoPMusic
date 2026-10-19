# PoP Music — Diseño y arquitectura

Guía de referencia para la app Android. Resume la identidad visual y el comportamiento
de la versión web ya desplegada, adaptados a un cliente nativo con reproducción real en
segundo plano.

## Principios

1. **La música primero**: la portada, el título y los controles no compiten con adornos.
2. **Oscuro absoluto**: fondo negro puro, superficies apenas elevadas, sin ruido visual.
3. **Contraste alto**: texto blanco sobre negro; el gris se reserva para metadatos.
4. **Movimiento sobrio**: transiciones cortas (150–250 ms), sin rebotes exagerados.
5. **Una mano**: acciones principales al alcance del pulgar; mini player siempre visible.

## Tokens

### Color

| Token        | Hex       | Uso                                  |
| ------------ | --------- | ------------------------------------ |
| `black`      | `#000000` | Fondo global                         |
| `bg-elev`    | `#0A0A0B` | Fondo elevado / contenedores         |
| `surface`    | `#0D0D0F` | Tarjetas y barras                    |
| `surface-2`  | `#17171A` | Mini player, campos, chips           |
| `surface-3`  | `#232327` | Indicador seleccionado, hover        |
| `line`       | `#1D1D21` | Separadores finos                    |
| `line-strong`| `#2A2A30` | Bordes de campos                     |
| `ink`        | `#FFFFFF` | Texto principal / botón primario     |
| `muted`      | `#B4B4BB` | Texto secundario                     |
| `faint`      | `#74747C` | Metadatos, iconos inactivos          |
| `accent`     | `#D7D7DE` | Acento grafito                       |
| `danger`     | `#FF4D4D` | Errores                              |

### Tipografía

- Familia: **Inter** (en nativo se usa `FontFamily.SansSerif` hasta empaquetar la fuente).
- Display: 48/52, semibold, tracking −1.
- Headline: 30/36 y 24/30, semibold, tracking negativo.
- Title: 20/26 y 16/22.
- Body: 16/24 y 14/20.
- Label: 13/18 y 11/16, tracking positivo en mayúsculas.

### Forma y espacio

- Radios: 4 · 8 · 12 · 16 · 20 px. Portadas 14–22 px; chips y botones circulares al 50 %.
- Escala de espacio: 4 · 8 · 12 · 16 · 20 · 24 · 32 · 40.
- Margen lateral de pantalla: 16 px (20 px en detalle y ajustes).
- Altura de fila de pista: 64 px. Mini player: 62 px + 6 px de separación.

### Cristal

Solo en overlays (hoja de cola, letras): `alpha 0.82`, `blur 24 px`, borde `line`.
En Android se implementará con `Brush` + `Modifier.blur` o un `Surface` semitransparente.

## Navegación

Barra inferior con 4 destinos:

| Destino    | Ruta        | Icono        |
| ---------- | ----------- | ------------ |
| Inicio     | `home`      | Casa         |
| Buscar     | `search`    | Lupa         |
| Reconocer  | `recognize` | Micrófono    |
| Biblioteca | `library`   | Biblioteca   |

Rutas de pila: `now_playing`, `playlist/{id}`, `settings`. El mini player se muestra en
todas las rutas principales y abre `now_playing`; allí desaparece la barra inferior.

## Pantallas

### Inicio
- Saludo temporal (“Buenas noches”) + acceso a Ajustes.
- Carrusel horizontal de mezclas/sugerencias.
- Lista “Escuchado recientemente”.
- Estados: carga con skeletons, vacío (descubre), error con reintento.

### Buscar
- Campo de búsqueda con acción de teclado.
- Chips de géneros sugeridos.
- Resultados agrupados: canciones, álbumes, artistas, playlists.
- Estado vacío con explicación; estado sin resultados.

### Reconocer
- Botón circular grande con animación de pulso.
- Flujo: permiso de micrófono → grabar ~8 s → AudD → buscar coincidencia → abrir reproductor.
- Estados: escuchando, analizando, encontrado, no encontrado, error de red.

### Biblioteca
- Pestañas: Todo · Playlists · Canciones · Artistas.
- Acciones: crear playlist, ordenar, buscar dentro de la biblioteca.
- Estados vacíos por pestaña.

### Playlist
- Cabecera con degradado según portada, título, contador y botón de reproducción.
- Lista ordenable; menú por pista (reproducir, añadir a cola, quitar, descargar).
- Acciones: renombrar, eliminar, exportar.

### Ahora suena
- Fondo degradado desde la portada.
- Portada grande, título y artista.
- Barra de progreso con tiempos; arrastre para buscar.
- Controles: aleatorio · anterior · reproducir/pausar · siguiente · repetir.
- Acciones secundarias: favorito, letras, cola.
- Cabecera: cerrar (abajo) y menú.

### Cola
- Hoja inferior con “Reproduciendo ahora” y “A continuación”.
- Reordenar, eliminar, vaciar. Repetir y aleatorio reflejan el estado real.

### Ajustes
- Reproducción: transición, normalizar volumen, letras sincronizadas.
- Datos: solo Wi-Fi, calidad, caché.
- Acerca de: versión, licencia GPLv3, fuentes.

## Componentes compartidos

- `Artwork`: portada con degradado de reserva e icono de nota.
- `TrackRow`: portada 52, título, artista y menú.
- `MediaCard`: portada cuadrada 148 + dos líneas.
- `SectionHeader`: título + acción opcional.
- `MiniPlayer`: portada, título, artista, play/pausa y siguiente.
- `EmptyState`: icono, título y descripción centrados.

## Reproducción (Media3)

- `MediaSessionService` + `ExoPlayer` para audio en segundo plano.
- Notificación y pantalla de bloqueo con metadatos y controles reales
  (anterior, siguiente, repetir, aleatorio, posición).
- Cola gestionada por la app; `setMediaItems` al cambiar de contexto.
- Fuente de audio: resolver URL con NewPipeExtractor y entregar a ExoPlayer.
- Manejo de errores: reintento, saltar pista y aviso no bloqueante.

## Datos

- **NewPipeExtractor**: búsqueda y streams de YouTube (GPLv3).
- **AudD**: reconocimiento de audio.
- **LrcLib**: letras sincronizadas.
- **Room**: biblioteca, playlists, historial y descargas.
- **DataStore**: ajustes.
- **Coil**: portadas y caché de imágenes.

## Estados y límites

- Sin conexión: mostrar biblioteca local y deshabilitar búsqueda remota.
- YouTube cambia con frecuencia: mantener actualizado NewPipeExtractor.
- La app es GPLv3 y no puede distribuirse como producto cerrado.
- El scraping de YouTube es una zona gris respecto a sus términos de servicio.
