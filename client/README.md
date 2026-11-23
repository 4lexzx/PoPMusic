# Cliente limpio de PoP Music

Proyecto **nuevo y original**, escrito desde cero. No deriva del código de Liner:
tiene su propia arquitectura, diseño e iconos. Existe para sustituir con el tiempo
al cliente de `desktop/` sin haber tocado nunca ese directorio.

## Funciones actuales

- Búsqueda musical a través del backend propio en Vercel: solo canciones,
  carátulas de álbum reales, sin videos.
- **Audio completo** resuelto localmente con `yt-dlp` desde el proceso principal
  de Electron (sin CORS y sin servicios de Liner). Si no hay Electron, cae al
  endpoint del backend y, por último, al preview de 30 s de iTunes.
- Biblioteca local: favoritos, recientes y playlists (crear, añadir, quitar).
  Todo se guarda en `localStorage`.
- Letras desde LrcLib vía el backend.
- Límite de errores: si la interfaz falla, muestra un mensaje y permite reiniciar.

## Arrancar

```bash
cd client
npm install
npm run dev            # navegador en http://localhost:5180
npm run electron       # ventana de Electron (requiere npm run dev aparte)
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
