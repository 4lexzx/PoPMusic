/**
 * Servidor local del backend de PoP Music.
 * Para producción en Vercel se usa api/[...path].js con la misma lógica.
 */
import http from "node:http";
import { handleRequest } from "./lib/handler.mjs";

const PORT = Number(process.env.PORT || 8787);

http.createServer(handleRequest).listen(PORT, () => {
  console.log(`PoP Music backend escuchando en http://localhost:${PORT}`);
});
