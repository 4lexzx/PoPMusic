import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const desktopDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const rustDir = path.join(desktopDir, 'rust')
const pkgDir = path.join(desktopDir, 'src', 'wasm', 'pkg')
const prebuiltDir = path.join(rustDir, 'prebuilt')
const wasmDir = path.join(desktopDir, 'src', 'wasm')
const templateFile = path.join(wasmDir, 'template-wasm.ts')
const fallbackFile = path.join(wasmDir, 'fallback.ts')
const indexFile = path.join(wasmDir, 'index.ts')

fs.mkdirSync(pkgDir, { recursive: true })

function copyRecursively(from, to) {
  for (const entry of fs.readdirSync(from, { withFileTypes: true })) {
    const source = path.join(from, entry.name)
    const target = path.join(to, entry.name)
    if (entry.isDirectory()) {
      fs.mkdirSync(target, { recursive: true })
      copyRecursively(source, target)
    } else if (entry.isFile()) {
      fs.mkdirSync(path.dirname(target), { recursive: true })
      fs.copyFileSync(source, target)
    }
  }
}

function usePrebuilt(message) {
  if (message) console.log(message)
  if (fs.existsSync(prebuiltDir)) {
    console.log('Copiando WASM precompilado desde rust/prebuilt/ …')
    copyRecursively(prebuiltDir, pkgDir)
    console.log('WASM precompilado copiado a src/wasm/pkg')
    fs.copyFileSync(templateFile, indexFile)
    return
  }
  console.warn('Rust/wasm-pack no disponible: se usa el fallback JS')
  fs.copyFileSync(fallbackFile, indexFile)
}

const result = spawnSync(
  'wasm-pack',
  ['build', '--target', 'web', '--release', '--out-dir', '../src/wasm/pkg'],
  {
    cwd: rustDir,
    shell: process.platform === 'win32',
    stdio: 'inherit',
  },
)

if (result.error) {
  if (result.error.code === 'ENOENT') {
    usePrebuilt('wasm-pack no está instalado.')
  } else {
    usePrebuilt(`No se pudo ejecutar wasm-pack: ${result.error.message}`)
  }
} else if (result.status !== 0) {
  usePrebuilt(`wasm-pack terminó con código ${result.status}.`)
} else {
  console.log('WASM compilado correctamente')
  fs.copyFileSync(templateFile, indexFile)
}
