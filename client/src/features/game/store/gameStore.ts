import { create } from 'zustand';

import { gameService } from '../services/gameService';
import type { GameStateDTO } from '../types/game';

interface GameStoreState {
  game: GameStateDTO | null;
  isBusy: boolean;
  error: string | null;
  hasInitialized: boolean;
  initializeGame: () => Promise<void>;
  drawCard: (_playerId?: string) => Promise<void>;
  stand: (_playerId?: string) => Promise<void>;
  nextRound: () => Promise<void>;
  restartGame: () => Promise<void>;
  dismissDuplicateAlert: () => void;
}

async function withGuard<T>(handler: () => Promise<T>, onError: (message: string) => void): Promise<T | undefined> {
  try {
    return await handler();
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unexpected game service failure';
    onError(message);
    return undefined;
  }
}

export const useGameStore = create<GameStoreState>((set, get) => ({
  game: null,
  isBusy: false,
  error: null,
  hasInitialized: false,

  initializeGame: async () => {
    if (get().hasInitialized || get().isBusy) {
      return;
    }

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const response = await gameService.startGame();
      set({ game: response.game, hasInitialized: true });
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  drawCard: async (playerId?: string) => {
    const game = get().game;

    if (!game) {
      return;
    }

    const targetPlayerId = playerId ?? game.currentTurnPlayerId;

    if (!targetPlayerId) {
      return;
    }

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const response = await gameService.drawCard(targetPlayerId);
      set({ game: response.game });
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  stand: async (playerId?: string) => {
    const game = get().game;

    if (!game) {
      return;
    }

    const targetPlayerId = playerId ?? game.currentTurnPlayerId;

    if (!targetPlayerId) {
      return;
    }

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const response = await gameService.stand(targetPlayerId);
      set({ game: response.game });
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  nextRound: async () => {
    if (!get().game) {
      return;
    }

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const response = await gameService.nextRound();
      set({ game: response.game });
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  restartGame: async () => {
    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const response = await gameService.restart();
      set({ game: response.game, hasInitialized: true });
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  dismissDuplicateAlert: () => {
    const game = get().game;

    if (!game) {
      return;
    }

    set({
      game: {
        ...game,
        duplicateAlert: null,
      },
    });
  },
}));
