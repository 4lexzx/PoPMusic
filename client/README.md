# Cliente limpio de PoP Music

Proyecto **nuevo y original**, escrito desde cero. No deriva del código de Liner:
tiene su propia arquitectura, diseño e iconos. Existe para sustituir con el tiempo
al cliente de `desktop/` sin haber tocado nunca ese directorio.

## Estado (fase 1)

- ✅ Búsqueda musical a través del backend propio en Vercel: solo canciones,
  carátulas de álbum reales, sin videos.
- ✅ Reproducción (por ahora usa el preview de 30 s de iTunes; el endpoint
  `/v1/audio` del backend ya está preparado para el audio completo).
- ✅ Favoritos guardados en el navegador.
- ✅ Letras desde LrcLib vía el backend.
- ⏳ Colas, historial, biblioteca, playlists, atajos, integración con Discord,
  salas, etc. (los tiene el cliente antiguo; faltan aquí).

## Arrancar

```bash
cd client
npm install
npm run dev
```

Abre `http://localhost:5180`. Para compilar: `npm run build`.

## Backend

Usa `https://backend-theta-one-73.vercel.app` por defecto. Se puede cambiar con
`VITE_API_URL`.

## Licencia

MIT. Código original de PoP Music.
