# Liner · Música

Reproductor de música web **mobile-first**, inspirado en [Liner](https://tryliner.fun). Funciona
**100 % en el navegador**, sin backend propio (salvo una Serverless Function opcional para Spotify)
y se despliega en **Vercel gratis**.

![stack](https://img.shields.io/badge/React-19-61dafb) ![stack](https://img.shields.io/badge/TypeScript-5-3178c6) ![stack](https://img.shields.io/badge/Tailwind-4-38bdf8) ![stack](https://img.shields.io/badge/Vite-6-646cff)

---

## ✨ Características

- **Diseño oscuro con vidrio 3D (glassmorphism)**, inspirado en la interfaz de Liner: negro puro,
  paneles translúcidos con `blur(20px) saturate(160%)`, profundidad por sombras y grano sutil. Barra
  de navegación inferior flotante (Inicio · Buscar · Biblioteca · Sonando).
- **Reproductor de YouTube** mediante IFrame Player API con iframe oculto y UI propia.
- **Controles completos**: play/pausa, siguiente/anterior, aleatorio, repetir, barra de progreso,
  volumen y cola de reproducción.
- **Letras sincronizadas** tipo karaoke (línea y palabra activas) desde **LRCLIB**, con animación de
  difuminado/escala en las líneas inactivas, control de desfase (offset ±10 s) y modo pantalla
  completa con fondo dinámico reactivo al color de la portada.
- **Botón «+» en la Biblioteca**: crear una playlist vacía o **importar** playlists de YouTube
  (Data API v3) y Spotify (metadatos vía Web API), todo desde una hoja deslizable.
- **Ajustes**: color de acento personalizable (6 opciones) y activación del fondo dinámico.
- **Biblioteca local**: playlists (con portada en mosaico y añadido/quitado de canciones), favoritos
  e historial guardados en `localStorage`.
- **Búsqueda** de canciones/artistas en YouTube, y traducción automática de pistas de Spotify
  a YouTube para su reproducción.
- **Modo demo** automático si no configuras claves: la app es totalmente navegable y reproducible.
- Integración con **Media Session** (controles en pantalla de bloqueo, cuando el navegador lo permite).

---

## 🧱 Stack técnico

| Área            | Tecnología                                        |
| --------------- | ------------------------------------------------- |
| UI              | React 19 + TypeScript                             |
| Estilos         | Tailwind CSS v4 (`@tailwindcss/vite`)             |
| Estado global   | Zustand (con `persist`)                           |
| Rutas           | React Router v6                                   |
| Audio           | YouTube IFrame Player API                         |
| Búsqueda        | YouTube Data API v3                               |
| Letras          | LRCLIB (`https://lrclib.net/api`)                 |
| Playlists       | YouTube Data API v3 + Spotify Web API (serverless) |
| Bundler         | Vite 6                                            |
| Deploy          | Vercel (SPA + funciones en `/api`)                |

---

## 🚀 Puesta en marcha

### Requisitos

- Node.js 20 o superior (probado con Node 22/24)
- npm

### Instalación

```bash
npm install
cp .env.example .env   # en Windows: copy .env.example .env
```

### Variables de entorno

| Variable                  | Obligatoria | Dónde se usa               | Descripción                                        |
| ------------------------- | ----------- | -------------------------- | -------------------------------------------------- |
| `VITE_YT_API_KEY`         | Opcional    | Navegador                  | Clave de YouTube Data API v3 (búsqueda e import).  |
| `VITE_SPOTIFY_CLIENT_ID`  | Opcional    | Cliente / servidor         | Client ID de Spotify (solo identificador).         |
| `SPOTIFY_CLIENT_SECRET`   | Opcional    | **Servidor** (`/api`)      | Client Secret de Spotify. **Nunca** va al cliente. |

> La app funciona **sin claves** en *modo demo*: podrás reproducir canciones de muestra y probar
> toda la interfaz. Las claves se leen en tiempo de build.

### Obtener las claves (gratis)

**YouTube Data API v3**

1. Entra en [Google Cloud Console](https://console.cloud.google.com/) y crea un proyecto.
2. Habilita *YouTube Data API v3* en **APIs y servicios → Biblioteca**.
3. Crea una credencial **Clave de API** en **Credenciales**.
4. (Recomendado) Restríngela a "Sitios web" con tus dominios.
5. Pégala en `VITE_YT_API_KEY`.

**Spotify Web API** (solo metadatos de playlists)

1. Crea una app en [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
2. Copia el **Client ID** en `VITE_SPOTIFY_CLIENT_ID` y el **Client Secret** en
   `SPOTIFY_CLIENT_SECRET`.

### Desarrollo

```bash
npm run dev
```

El servidor de Vite emula la función `/api/spotify` en desarrollo, así que puedes importar
playlists de Spotify con `npm run dev` sin instalar nada más.

Abre `http://localhost:5173` (o usa la IP de red para probar en tu móvil).

### Build de producción

```bash
npm run build     # tsc --noEmit && vite build
npm run preview   # sirve la carpeta dist localmente
```

---

## ☁️ Deploy en Vercel (gratis)

1. Sube el repositorio a GitHub/GitLab.
2. En [Vercel](https://vercel.com/new) importa el repositorio. Detecta Vite automáticamente
   (gracias a `vercel.json`).
3. En **Settings → Environment Variables** añade:
   - `VITE_YT_API_KEY`
   - `VITE_SPOTIFY_CLIENT_ID`
   - `SPOTIFY_CLIENT_SECRET` (marca que es sensible)
4. Deploy. La función `api/spotify.ts` se publica como `/api/spotify` automáticamente.

> `vercel.json` incluye el rewrite de SPA (todas las rutas → `index.html`) excluyendo `/api/*`,
> y cabeceras de caché para los assets.

También puedes usar la CLI:

```bash
npm i -g vercel
vercel        # preview
vercel --prod # producción
```

---

## 📁 Estructura del proyecto

```
api/
  spotify.ts                 # Serverless Function de Vercel (Spotify, Client Credentials)
server/
  spotify.ts                 # Lógica compartida Spotify (usada por la función y por Vite en dev)
public/                      # favicon y manifiesto PWA
src/
  components/                # UI reutilizable (Button, Slider, Sheet, PillTabs, Artwork, Logo, Icons, Layout)
  features/
    player/                  # Reproductor YouTube, controles, mini reproductor, cola
    lyrics/                  # Hook de letras y vistas sincronizadas / karaoke con offset
    library/                 # Tarjetas, filas de pistas, hoja «+» (crear/importar) y añadir canciones
    search/                  # Resultados de búsqueda
    settings/                # Hoja de ajustes (acento de color y fondo dinámico)
    visuals/                 # Fondo dinámico reactivo a la portada
  shared/
    api/                     # Clientes: youtube.ts, lrclib.ts, spotify.ts, http.ts
    store/                   # Zustand: playerStore, libraryStore, uiStore, settingsStore
    types/                   # Tipos TypeScript
    utils/                   # lrc.ts, color.ts, text.ts, time.ts
  pages/                     # Home, Search, Library, NowPlaying, Playlist
  App.tsx
  main.tsx
```

Rutas de la app: `/` · `/search` · `/library` · `/now-playing` · `/playlist/:id`.

---

## ⚠️ Limitaciones y notas

- **Reproducción en segundo plano**: en móvil (iOS Safari / Chrome Android) el navegador puede
  pausar el iframe oculto al pasar a segundo plano. Se integra *Media Session* para ofrecer
  controles en la pantalla de bloqueo, pero no se garantiza el audio continuo en todos los equipos.
- **Cuota de YouTube Data API**: 10 000 unidades/día. Una búsqueda cuesta ~100 unidades
  (≈100 búsquedas/día). Importar playlists y resolver pistas de Spotify consumen menos.
- **Spotify**: desde nov-2024 Spotify restringe varios endpoints para apps nuevas. La app obtiene
  los metadatos de la playlist vía función serverless y, si Spotify no permite listar las canciones,
  lo informa. La reproducción de Spotify no está soportada: las pistas de Spotify se resuelven
  automáticamente buscándolas en YouTube (`"[título] [artista] official"`).
- **Letras (LRCLIB)**: base de datos colaborativa; algunos registros pueden ser incorrectos o
  contener solo letra sin sincronizar. Si no hay LRC, se muestra la letra estática.
- **Karaoke por palabra**: si el LRC solo trae tiempo por línea, la app reparte las palabras dentro
  de la duración de la línea para animarlas de forma aproximada.
- **Uso de YouTube**: la app incrusta el reproductor oficial mediante la IFrame Player API,
  respetando sus términos de servicio.

---

## 📜 Scripts

| Script              | Descripción                                     |
| ------------------- | ----------------------------------------------- |
| `npm run dev`       | Servidor de desarrollo (incluye API de Spotify) |
| `npm run build`     | Chequeo de tipos + build de producción          |
| `npm run preview`   | Sirve el build de producción                    |
| `npm run typecheck` | Solo chequeo de tipos (`tsc --noEmit`)          |

---

Hecho con React, Tailwind y APIs gratuitas. Cero dependencias de pago.
