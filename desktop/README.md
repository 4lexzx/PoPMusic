# PoP Music para Windows

Reproductor de música de escritorio: biblioteca, búsqueda, letras sincronizadas,
reproductor flotante y fondo oscuro que sigue la carátula del disco.

## Desarrollo

```bash
npm install
npm run dev
```

## Compilar

```bash
npm run build
npx electron-builder --win --x64 --publish never
```

El instalador se genera en `release/`. Usa iconos propios y tema oscuro por defecto.

## Estructura

- `src/app` — arranque, rutas y estilos globales.
- `src/features` — reproductor, letras, biblioteca, búsqueda, ajustes y navegación.
- `src/shared` — API, contratos, almacenamiento, iconos y utilidades.
- `src/widgets` — marco de la ventana, barra del reproductor y paneles.
- `electron` — proceso principal, preload, overlays y atajos.

## Licencia

MIT. Ver [LICENSE](./LICENSE).
