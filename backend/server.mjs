/**
 * PoP Music — backend propio.
 *
 * Expone la misma forma de endpoints que espera el cliente de escritorio, pero
 * los datos salen de fuentes públicas de música (iTunes Search / LrcLib), no de
 * los servidores de Liner. Las búsquedas devuelven solo canciones, álbumes y
 * artistas: nunca videos, y las carátulas son arte de álbum a 600x600.
 *
 * Sin dependencias externas: usa http y fetch del propio Node.
 */
import http from "node:http";

const PORT = Number(process.env.PORT || 8787);
const ITUNES_API = "https://itunes.apple.com";
const LRCLIB_API = "https://api.lrclib.net";
const TIMEOUT_MS = 8_000;

function cors(res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader(
    "Access-Control-Allow-Headers",
    "authorization, content-type, x-liner-signature, x-liner-timestamp, x-liner-nonce, x-popmusic-signature, x-popmusic-timestamp, x-popmusic-nonce",
  );
}

function send(res, status, body) {
  cors(res);
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(body));
}

async function fetchJson(url) {
  const response = await fetch(url, {
    signal: AbortSignal.timeout(TIMEOUT_MS),
    headers: { "User-Agent": "PoPMusic/1.0" },
  });
  if (!response.ok) throw new Error(`Fuente externa respondió ${response.status}`);
  return response.json();
}

function artwork(url, size = 600) {
  if (!url) return "";
  return url.replace(/\/100x100bb\./, `/${size}x${size}bb.`);
}

function mapSong(item) {
  const image = artwork(item.artworkUrl100);
  return {
    type: "track",
    id: String(item.trackId ?? ""),
    title: item.trackName ?? "",
    artists: item.artistName ?? "",
    artistList: item.artistName
      ? [{ id: String(item.artistId ?? ""), name: item.artistName }]
      : [],
    artistId: String(item.artistId ?? ""),
    coverUrl: image,
    cover: image ? { url: image, width: 600, height: 600 } : undefined,
    durationMs: item.trackTimeMillis ?? 0,
    album: {
      id: String(item.collectionId ?? ""),
      title: item.collectionName ?? "",
    },
    explicit: item.trackExplicitness === "explicit",
  };
}

function mapAlbum(item) {
  const image = artwork(item.artworkUrl100);
  return {
    type: "album",
    id: String(item.collectionId ?? ""),
    title: item.collectionName ?? "",
    artists: [{ id: String(item.artistId ?? ""), name: item.artistName ?? "" }],
    year: item.releaseDate ? Number(item.releaseDate.slice(0, 4)) : undefined,
    cover: image ? { url: image, width: 600, height: 600 } : undefined,
  };
}

function mapArtist(item) {
  return {
    type: "artist",
    id: String(item.artistId ?? ""),
    name: item.artistName ?? "",
  };
}

async function itunesSearch(term, entity, country, limit) {
  const url = new URL(`${ITUNES_API}/search`);
  url.searchParams.set("term", term);
  url.searchParams.set("entity", entity);
  url.searchParams.set("country", country);
  url.searchParams.set("limit", String(limit));
  const data = await fetchJson(url.toString());
  return Array.isArray(data.results) ? data.results : [];
}

async function itunesLookup(id, entity = "song") {
  const url = new URL(`${ITUNES_API}/lookup`);
  url.searchParams.set("id", String(id));
  url.searchParams.set("entity", entity);
  url.searchParams.set("limit", "200");
  const data = await fetchJson(url.toString());
  return Array.isArray(data.results) ? data.results : [];
}

async function handleSearch(url, res) {
  const q = (url.searchParams.get("q") ?? "").trim();
  const type = url.searchParams.get("type") ?? "all";
  const country = url.searchParams.get("country") ?? "US";
  if (q.length < 2) return send(res, 200, { items: [] });

  const items = [];
  if (type === "track" || type === "all") {
    const songs = await itunesSearch(q, "song", country, 50);
    items.push(
      ...songs
        .filter((s) => s.kind === "song" || s.wrapperType === "track")
        .map(mapSong),
    );
  }
  if (type === "album" || type === "all") {
    const albums = await itunesSearch(q, "album", country, 25);
    items.push(...albums.map(mapAlbum));
  }
  if (type === "artist" || type === "all") {
    const artists = await itunesSearch(q, "musicArtist", country, 10);
    items.push(...artists.map(mapArtist));
  }
  return send(res, 200, { items });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url ?? "/", `http://${req.headers.host ?? "localhost"}`);
  const path = url.pathname.replace(/\/+$/, "");

  if (req.method === "OPTIONS") {
    cors(res);
    res.writeHead(204);
    return res.end();
  }

  try {
    if (path === "/health" && req.method === "GET")
      return send(res, 200, { ok: true, service: "popmusic-backend" });

    if (path === "/v1/catalog/search" && req.method === "GET")
      return await handleSearch(url, res);

    const trackMatch = path.match(/^\/v1\/catalog\/tracks\/(.+)$/);
    if (trackMatch && req.method === "GET") {
      const results = await itunesLookup(trackMatch[1], "song");
      const found = results.find((r) => r.wrapperType === "track");
      if (!found) return send(res, 404, { code: "NOT_FOUND" });
      return send(res, 200, mapSong(found));
    }

    const albumMatch = path.match(/^\/v1\/catalog\/albums\/(.+)$/);
    if (albumMatch && req.method === "GET") {
      const results = await itunesLookup(albumMatch[1], "song");
      const cover = results[0];
      if (!cover) return send(res, 404, { code: "NOT_FOUND" });
      return send(res, 200, {
        ...mapAlbum(cover),
        tracks: results.filter((r) => r.wrapperType === "track").map(mapSong),
      });
    }

    const artistMatch = path.match(/^\/v1\/catalog\/artists\/(.+)$/);
    if (artistMatch && req.method === "GET") {
      const results = await itunesLookup(artistMatch[1], "album");
      return send(res, 200, {
        ...mapArtist(results[0] ?? { artistId: artistMatch[1] }),
        albums: results.filter((r) => r.wrapperType === "collection").map(mapAlbum),
      });
    }

    if (path === "/v1/lyrics" && req.method === "GET") {
      const track = url.searchParams.get("track") ?? "";
      const artist = url.searchParams.get("artist") ?? "";
      if (!track) return send(res, 200, { lyrics: null });
      const search = new URL(`${LRCLIB_API}/tracksearch`);
      search.searchParams.set("track_name", track);
      if (artist) search.searchParams.set("artist_name", artist);
      let best = null;
      try {
        const rows = await fetchJson(search.toString());
        best = Array.isArray(rows) ? rows[0] : null;
      } catch {
        // LrcLib no está disponible en esta red: mejor devolver null que un 502.
      }
      return send(res, 200, {
        lyrics: best?.syncedLyrics ?? best?.plainLyrics ?? null,
      });
    }

    // El cliente pide el audio aquí. No lo servimos: cae al resolutor de
    // YouTube del propio cliente, que ya funciona sin ningún servicio externo.
    if (path.includes("/playback"))
      return send(res, 404, { code: "PLAYBACK_NOT_FOUND" });

    // El firmante del cliente espera una clave de borde; no la verificamos.
    if (path === "/v1/edge/key")
      return send(res, 200, {
        key: "popmusic-local",
        rotatesAt: new Date(Date.now() + 86_400_000).toISOString(),
      });

    // Sin cuenta no hay biblioteca que sincronizar: listados vacíos y
    // respuestas correctas para que la interfaz no muestre errores.
    if (path.startsWith("/v1/me/") && req.method === "GET")
      return send(res, 200, { items: [] });
    if (path === "/v1/me")
      return send(res, 200, { id: "local", username: "local" });
    if (path.startsWith("/v1/explore/"))
      return send(res, 200, { items: [] });

    return send(res, 404, { code: "NOT_FOUND", path });
  } catch (error) {
    return send(res, 502, { code: "UPSTREAM_ERROR", message: String(error) });
  }
});

server.listen(PORT, () => {
  console.log(`PoP Music backend escuchando en http://localhost:${PORT}`);
});
