# Backend de PoP Music

Servicio propio que expone la misma forma de API que espera el cliente de
escritorio, pero sin depender de los servidores de Liner. Fuentes de datos:

- **iTunes Search API** — catálogo musical: solo canciones, álbumes y artistas,
  con carátulas de álbum reales a 600x600.
- **LrcLib** — letras sincronizadas.

La reproducción no pasa por aquí: el cliente resuelve el audio directamente con
su propio resolutor de YouTube.

## Arrancar

```bash
cd backend
npm start
```

Escucha en `http://localhost:8787`. Variables de entorno:

- `PORT` — puerto (por defecto `8787`).

## Probar

```bash
curl http://localhost:8787/health
curl "http://localhost:8787/v1/catalog/search?q=luis%20miguel&type=track"
```

## Conectar el escritorio

En `desktop/.env.local`:

```
VITE_API_URL=http://localhost:8787
```

(créalo a partir de `desktop/.env.example`). Tras cambiarlo hay que recompilar
el instalador. Si no se define, el cliente sigue usando el valor por defecto.
