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

## Release firmado

1. Crea el keystore y `keystore.properties` (ambos ignorados por git; **haz copia de
   seguridad**, sin ellos no podrás actualizar la app):

   ```bash
   keytool -genkeypair -v -keystore keystore/popmusic-release.jks -alias popmusic \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

   ```properties
   # keystore.properties
   storeFile=keystore/popmusic-release.jks
   storePassword=...
   keyAlias=popmusic
   keyPassword=...
   ```

2. Compila y verifica:

   ```bash
   ./gradlew assembleRelease
   "$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```

El APK firmado queda en `app/build/outputs/apk/release/app-release.apk`.

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
- [x] APK de release firmado (v0.1.0)
- [ ] R8/minify y perfil de baseline para reducir tamaño
- [ ] Firma de release en CI

## Licencia

GPLv3. Ver [`LICENSE`](LICENSE).
