# pop_wasm
Este crate se compila a WebAssembly en GitHub Actions y se publica en `desktop/src/wasm/pkg`.
Localmente, sin Rust ni wasm-pack instalados, la app usa el fallback JS (`src/shared/utils/lrc.ts` y `color.ts`).
Para compilarlo a mano: `wasm-pack build --target web --release --out-dir ../src/wasm/pkg` (desde `desktop/rust`).
También puedes dejar un build previo en `rust/prebuilt/`; `desktop/scripts/build-wasm.mjs` lo copiará automáticamente.
