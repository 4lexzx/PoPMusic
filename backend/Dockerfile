FROM node:22-slim

# yt-dlp necesita Python; ffmpeg ayuda con algunos formatos.
RUN apt-get update \
  && apt-get install -y --no-install-recommends python3 python3-pip ffmpeg ca-certificates \
  && rm -rf /var/lib/apt/lists/* \
  && pip3 install --break-system-packages --no-cache-dir yt-dlp

WORKDIR /app
COPY package.json ./
COPY server.mjs ./

ENV NODE_ENV=production
ENV PORT=8080
EXPOSE 8080

CMD ["node", "server.mjs"]
