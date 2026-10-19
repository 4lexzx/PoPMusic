# Backend de audio — PoP Music

Servicio que **extrae el audio de YouTube con [yt-dlp]** y lo sirve como stream
directo. Así la web reproduce con un `<audio>` real y la música **continúa al
salir de la app** (segundo plano y pantalla de bloqueo), cosa que un iframe de
YouTube no permite.

## Requisitos

- Node.js 20+
- [`yt-dlp`](https://github.com/yt-dlp/yt-dlp) en el PATH (o `YTDLP_PATH`)
- `ffmpeg` recomendado (algunos formatos)

## Ejecutar en local

```bash
# 1) instala yt-dlp (ejemplo con pip; también hay binario único)
pip install -U yt-dlp

# 2) arranca el backend
cd backend
npm start
# -> pop-music audio backend escuchando en http://0.0.0.0:8080
```

Comprueba:

```bash
curl "http://localhost:8080/health"
curl -o prueba.m4a "http://localhost:8080/stream?videoId=dQw4w9WgXcQ"
```

Y en el `.env` de la web:

```
VITE_AUDIO_API=http://localhost:8080
```

## Desplegar en un VPS (Docker — recomendado)

```bash
cd backend
docker build -t popmusic-audio .
docker run -d --name popmusic-audio \
  -p 8080:8080 \
  -e ALLOW_ORIGIN=https://popmusic-black.vercel.app \
  --restart unless-stopped \
  popmusic-audio
```

Ponlo detrás de HTTPS (nginx/Caddy) y apunta la web a ese dominio:

```
VITE_AUDIO_API=https://audio.tudominio.com
```

> El navegador solo acepta audio desde `https` cuando la web es `https`, así que
> el backend **debe** ir con HTTPS en producción (Caddy hace el certificado solo).

## Desplegar sin Docker (systemd)

```bash
# en el servidor
sudo apt install -y python3-pip ffmpeg
pip3 install yt-dlp
git clone <tu-repo> /opt/popmusic-audio
```

`/etc/systemd/system/popmusic-audio.service`:

```ini
[Unit]
Description=PoP Music audio backend
After=network.target

[Service]
WorkingDirectory=/opt/popmusic-audio/backend
ExecStart=/usr/bin/node server.mjs
Environment=PORT=8080
Environment=ALLOW_ORIGIN=https://popmusic-black.vercel.app
Restart=always

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now popmusic-audio
```

## Variables de entorno

| Variable       | Por defecto | Descripción                                        |
| -------------- | ----------- | -------------------------------------------------- |
| `PORT`         | `8080`      | Puerto de escucha.                                 |
| `ALLOW_ORIGIN` | `*`         | Origen permitido para CORS. **En producción, restríngelo.** |
| `YTDLP_PATH`   | `yt-dlp`    | Ruta al binario `yt-dlp`.                          |
| `LOG_LEVEL`    | `info`      | `info` o `silent`.                                 |

## Endpoints

- `GET /health` → `{ "ok": true }`
- `GET /stream?videoId=<id>` → audio con soporte de `Range` (permite buscar dentro de la pista).

## Notas y límites

- Las URLs de audio de YouTube caducan (~horas); este backend las resuelve de
  nuevo y las cachea ~2 h.
- YouTube cambia a menudo: mantén `yt-dlp` actualizado (`pip install -U yt-dlp`)
  o reconstruye la imagen Docker de vez en cuando.
- Si un vídeo no está disponible (privado, con restricción de edad, bloqueo por
  región), el stream devuelve error y la web cae al reproductor de YouTube.

[yt-dlp]: https://github.com/yt-dlp/yt-dlp
