import type { PopDesktopApi } from "../electron/preload";

declare global {
  interface Window {
    popDesktop?: PopDesktopApi;
  }
}

export {};
