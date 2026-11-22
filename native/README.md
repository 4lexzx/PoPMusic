# PoP Music (Android)

Cliente de música nativo para Android: streaming sin anuncios de YouTube Music,
reproducción en segundo plano, letras sincronizadas, reconocimiento de canciones,
biblioteca y playlists.

## Stack

- Kotlin + Jetpack Compose + Material 3
- Media3 / ExoPlayer + `MediaSessionService` (cola real con `onAddMediaItems`)
- NewPipeExtractor (búsqueda, streams y playlists de YouTube)
- Biblioteca persistida en JSON local (favoritos y playlists)
- Interfaz *liquid glass*: superficies translúcidas con brillo y borde de luz,
  fondo difuminado, botones que se comprimen al pulsar (`pressScale`) y
  transiciones animadas con profundidad
- Personalización del fondo (degradados animados, imagen propia, desenfoque,
  oscurecimiento y transparencia del cristal) persistida en el dispositivo
- Letras estilo karaoke con seguimiento de la línea actual
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

### Supabase Auth

El login usa Supabase Auth desde la app Android. Añade estas claves en
`local.properties`:

```properties
supabase.url=https://TU-PROJECT-REF.supabase.co
supabase.anonKey=sb_publishable_TU_CLAVE
```

La URL debe ser exactamente el **Project URL** del dashboard: si
`TU-PROJECT-REF.supabase.co` no resuelve por DNS, el proyecto está pausado,
eliminado o el `ref` no es correcto.

En Supabase:

1. **Authentication > Providers > Email**: activa Email y, si quieres validar
   correos reales, deja activada la confirmación por correo.
2. **Authentication > Providers > Google**: activa Google con el Client ID y
   Client Secret de Google Cloud. El redirect URI de Google Cloud es
   `https://TU-PROJECT-REF.supabase.co/auth/v1/callback`.
3. **Authentication > URL Configuration > Redirect URLs**: añade
   `popmusic://auth-callback`.

La app abre Google en el navegador del sistema y vuelve mediante el deep link
`popmusic://auth-callback`. Los enlaces de confirmación de correo también vuelven
a la app si `popmusic://auth-callback` está autorizado como Redirect URL.

La app acepta los tres formatos de enlace que envía Supabase:

- tokens en el fragmento (`#access_token=...&type=recovery`);
- `?token_hash=...&type=recovery|signup` (verificados contra `/verify`);
- `?code=...` con PKCE (solo usado por Google).

El botón «¿Olvidaste tu contraseña?» usa `/recover` con el mismo redirect; al
volver, se abre el diálogo «Nueva contraseña» y se guarda con `PUT /user`.

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
- [x] Importar playlists de YouTube Music desde un enlace (botón **+** en Biblioteca)
- [x] Home con secciones reales de YouTube
- [x] Interfaz *liquid glass*, botones animados y transiciones con profundidad
- [x] Ajustes → Personalización: fondos animados, imagen de la galería y sliders
      de desenfoque / oscurecimiento / transparencia (con vista previa)
- [x] Letras con animación de seguimiento tipo karaoke
- [x] Portadas en alta resolución y títulos sin emojis
- [x] APK de release firmado (v0.1.0)
- [ ] Importación nativa de playlists de Spotify (requiere OAuth)
- [ ] R8/minify y perfil de baseline para reducir tamaño
- [ ] Firma de release en CI

## Licencia

GPLv3. Ver [`LICENSE`](LICENSE).
