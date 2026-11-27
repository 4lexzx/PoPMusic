# Cliente limpio de PoP Music

Proyecto **nuevo y original**, escrito desde cero. No deriva del código de Liner:
tiene su propia arquitectura, diseño e iconos. Existe para sustituir con el tiempo
al cliente de `desktop/` sin haber tocado nunca ese directorio.

## Funciones actuales

- **Inicio sin fin y solo música**: mezclas por artista, "Porque escuchas…",
  artistas que te pueden gustar, tops del país y mundiales, listas destacadas y
  géneros. Se arma con tus "Me gusta" y lo que escuchas, nunca repite canciones
  y sigue cargando mientras bajas. Catálogo de Deezer (carátulas hasta 1400 px
  y fotos de artistas); iTunes para el top por país.
- **Audio completo** con `yt-dlp` desde el proceso principal de Electron. Se
  prepara antes del clic (al pasar el ratón, primer resultado de búsqueda,
  siguientes de la cola) y se guarda en disco hasta que caduca el enlace.
  Al acabar la cola sigue con canciones parecidas.
- **Letras palabra por palabra** sincronizadas a 60 fps: tiempos reales de
  LrcLib o NetEase (o estimados por sílabas) y cinco efectos (Karaoke, Brillo,
  Rebote, Aparecer, Por línea).
- **Buscador** en el propio inicio (el fondo se desenfoca) y página completa
  con canciones, álbumes y artistas.
- **Reproductor flotante** y **superposición** siempre encima al minimizar
  (Alt+Shift+O). Teclas multimedia de Windows.
- **Temas** claro, oscuro o del sistema y color de énfasis con contraste
  automático. Ajustes en una ventana con buscador.
- Biblioteca local: me gusta, recientes y playlists, en `localStorage`.

## Arrancar

```bash
cd client
npm install
npm run dev            # navegador en http://localhost:5180
npm run electron:dev   # ventana de Electron (requiere npm run dev aparte)
npm start              # compila y abre Electron sin servidor de desarrollo
```

## Generar instalador

```bash
npm run dist:win
```

El EXE se crea en `client/release/`.

## Backend

Usa `https://backend-theta-one-73.vercel.app` por defecto. Se puede cambiar con
`VITE_API_URL`.

## Licencia

MIT. Código original de PoP Music.
