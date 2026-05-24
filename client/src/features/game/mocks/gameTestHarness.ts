import type { GameTestStateConfig } from '../types/game';

declare global {
  interface Window {
    __FLIP7_TEST__?: {
      state?: GameTestStateConfig;
    };
  }
}

export function getGameTestConfig() {
  return window.__FLIP7_TEST__?.state;
}

