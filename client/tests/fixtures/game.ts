/* eslint-disable react-hooks/rules-of-hooks */
import { test as base, expect, type Page } from '@playwright/test';

import { buildGameState, startMockGame } from '../helpers/game';
import type { GameTestStateConfig } from '../../src/features/game/types/game';

type GameFixtures = {
  loadMockGame: (state?: Partial<GameTestStateConfig>) => Promise<Page>;
};

export const test = base.extend<GameFixtures>({
  loadMockGame: async ({ page }, use) => {
    await use(async (state: Partial<GameTestStateConfig> = {}) => {
      await startMockGame(page, buildGameState(state));
      return page;
    });
  },
});

export { expect };
