# PoP Music (Android)

Cliente de música nativo para Android: streaming sin anuncios de YouTube Music,
reproducción en segundo plano, letras sincronizadas, reconocimiento de canciones,
biblioteca y playlists.

## Stack

- Kotlin + Jetpack Compose + Material 3
- Media3 / ExoPlayer + `MediaSessionService` (cola real con `onAddMediaItems`)
- NewPipeExtractor (búsqueda y streams de YouTube)
- Biblioteca persistida en JSON local (favoritos y playlists)
- Coil · GPLv3

## Requisitos

- Android Studio reciente (JDK incluido)
- SDK Android 37 (`compileSdk 37`), `minSdk 26`

## Claves

El reconocimiento de canciones usa AudD. Añade tu clave en `local.properties`
(ignorado por git):

```properties
audd.apiKey=tu_clave
```

## Compilar

```bash
cd native
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para ejecutar en un dispositivo: abre la carpeta `native/` en Android Studio y pulsa **Run**.

## Estructura

```
app/src/main/java/com/popmusic/app/
├─ ui/        temas, navegación, componentes y pantallas
├─ data/      YouTube (NewPipe), letras (LrcLib), reconocimiento (AudD) y biblioteca
├─ domain/    modelos (Song, Playlist, Lyrics, ...)
└─ player/    Media3, sesión multimedia y cola de reproducción
```

## Estado

- [x] Proyecto Gradle + Compose + tema
- [x] Navegación y pantallas
- [x] Búsqueda y resolución de audio con NewPipeExtractor
- [x] Reproductor Media3 + sesión multimedia (segundo plano)
- [x] Cola real: anterior/siguiente, aleatorio y repetir (incluye pantalla de bloqueo)
- [x] Letras sincronizadas con LrcLib
- [x] Reconocimiento de canciones con AudD
- [x] Biblioteca y playlists persistidas (favoritos, crear playlist, añadir/quitar)
- [x] Home con secciones reales de YouTube
- [ ] APK de release firmado

## Licencia

GPLv3. Ver [`LICENSE`](LICENSE).
