# PoP Music (Android)

Cliente de música nativo para Android: streaming sin anuncios de YouTube Music,
reproducción en segundo plano, letras sincronizadas, reconocimiento de canciones,
biblioteca y playlists.

## Stack

- Kotlin + Jetpack Compose + Material 3
- Media3 / ExoPlayer + `MediaSessionService`
- NewPipeExtractor (búsqueda y streams de YouTube)
- Room + DataStore · Ktor · Coil
- GPLv3

## Requisitos

- Android Studio reciente (JDK incluido)
- SDK Android 37 (`compileSdk 37`), `minSdk 26`

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
├─ data/      repositorios, Room, red (pendiente)
├─ domain/    modelos y casos de uso (pendiente)
├─ player/    Media3 y sesión multimedia (pendiente)
└─ di/        inyección de dependencias (pendiente)
```

## Estado

- [x] Proyecto Gradle + Compose + tema
- [x] Navegación y esqueleto de pantallas
- [ ] Capa de datos (NewPipeExtractor, Room, DataStore)
- [ ] Reproductor en segundo plano
- [ ] Letras sincronizadas y reconocimiento
- [ ] APK de release

## Licencia

GPLv3. Ver [`LICENSE`](LICENSE).
