import { expect, type Page } from '@playwright/test';

import type { GameTestPlayerConfig, GameTestStateConfig } from '../../src/features/game/types/game';

export function createMockDeck(values: number[]) {
  return [...values];
}

export function slugify(value: string) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, '-');
}

export function createPlayer(name: string, overrides: Partial<GameTestPlayerConfig> = {}): GameTestPlayerConfig {
  return {
    name,
    ...overrides,
  };
}

export function createDefaultPlayers() {
  return [
    createPlayer('Satoshi', { id: 'player-1', hasTurn: true }),
    createPlayer('Ada', { id: 'player-2' }),
    createPlayer('Marcus', { id: 'player-3' }),
    createPlayer('Elena', { id: 'player-4' }),
  ];
}

export function buildGameState(overrides: Partial<GameTestStateConfig> = {}): GameTestStateConfig {
  const players = overrides.players ?? createDefaultPlayers();
  const firstTurnPlayerId = overrides.currentTurnPlayerId ?? players.find((player) => player.hasTurn)?.id ?? players[0]?.id ?? null;

  return {
    currentRound: 1,
    currentTurnPlayerId: firstTurnPlayerId,
    roundStarterPlayerId: overrides.roundStarterPlayerId ?? firstTurnPlayerId,
    players,
    deck: overrides.deck ?? createMockDeck([2, 5, 9, 1, 8, 3, 6, 4]),
    discard: overrides.discard ?? [],
    events:
      overrides.events ?? [
        {
          title: 'Partida iniciada',
          description: 'La mesa está lista y la primera ronda ha comenzado.',
          tone: 'primary',
        },
      ],
    gamePhase: overrides.gamePhase ?? 'playing',
    roundSummary: overrides.roundSummary ?? null,
    duplicateAlert: overrides.duplicateAlert ?? null,
    winner: overrides.winner ?? null,
    riskLevel: overrides.riskLevel ?? 0,
  };
}

export async function startMockGame(page: Page, state: GameTestStateConfig) {
  await page.addInitScript((config) => {
    window.__FLIP7_TEST__ = { state: config };
  }, state);

  await page.goto('/multiplayer');

  await expect(page.getByTestId('event-log')).toBeVisible();
}

export function getPlayerCard(page: Page, playerName: string) {
  return page.getByTestId(`player-card-${slugify(playerName)}`);
}

export function getActivePlayerCard(page: Page) {
  return page.locator('[data-active-player="true"]').first();
}

export async function drawCurrentPlayer(page: Page) {
  await expect(page.getByTestId('draw-button')).toBeEnabled();
  await page.getByTestId('draw-button').click();
}

export async function standCurrentPlayer(page: Page) {
  await expect(page.getByTestId('stand-button')).toBeEnabled();
  await page.getByTestId('stand-button').click();
}

export async function finishRound(page: Page, maxTurns = 10) {
  for (let turn = 0; turn < maxTurns; turn += 1) {
    const summaryVisible = await page.getByTestId('round-summary-modal').isVisible().catch(() => false);
    const winnerVisible = await page.getByTestId('winner-modal').isVisible().catch(() => false);

    if (summaryVisible || winnerVisible) {
      return;
    }

    await expect(page.getByTestId('stand-button')).toBeEnabled();
    await standCurrentPlayer(page);
  }
}

export async function drawUntilDuplicate(page: Page) {
  await drawCurrentPlayer(page);
  await expect(page.getByTestId('duplicate-alert')).toBeVisible();
}

export async function acknowledgeDuplicateAlert(page: Page) {
  await page.getByTestId('duplicate-alert').getByRole('button', { name: /reconocer/i }).click();
  await expect(page.getByTestId('duplicate-alert')).toHaveCount(0);
}

export async function expectActivePlayer(page: Page, playerName: string) {
  await expect(getActivePlayerCard(page)).toContainText(playerName);
}
