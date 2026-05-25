import { test, expect } from '../fixtures/game';
import {
  createPlayer,
  createMockDeck,
  expectActivePlayer,
  standCurrentPlayer,
} from '../helpers/game';

test('stand action locks the player out of future turns and preserves the score', async ({ loadMockGame }) => {
  const page = await loadMockGame({
    players: [
      createPlayer('Satoshi', { id: 'player-1', hasTurn: true, roundCards: [2] }),
      createPlayer('Ada', { id: 'player-2' }),
      createPlayer('Marcus', { id: 'player-3' }),
      createPlayer('Elena', { id: 'player-4' }),
    ],
    currentTurnPlayerId: 'player-1',
    roundStarterPlayerId: 'player-1',
    deck: createMockDeck([2, 5, 7, 9]),
  });

  await standCurrentPlayer(page);
  await expectActivePlayer(page, 'Ada');
  await expect(page.getByTestId('player-card-satoshi')).toHaveClass(/stood/);

  await page.getByTestId('draw-button').click();
  await expectActivePlayer(page, 'Marcus');
  await expect(page.getByTestId('stand-button')).toBeEnabled();

  await standCurrentPlayer(page);
  await expectActivePlayer(page, 'Elena');
  await expect(page.getByTestId('stand-button')).toBeEnabled();

  await standCurrentPlayer(page);
  await expectActivePlayer(page, 'Ada');
  await expect(page.getByTestId('stand-button')).toBeEnabled();

  await standCurrentPlayer(page);

  await expect(page.getByTestId('round-summary-modal')).toBeVisible();
  await expect(page.getByTestId('round-summary-modal')).toContainText('Resumen de ronda');
  await expect(page.getByTestId('round-summary-modal')).toContainText('Satoshi');
  await expect(page.getByTestId('round-summary-modal')).toContainText('2');
  await expect(page.getByTestId('player-card-satoshi')).toContainText('2');
});
