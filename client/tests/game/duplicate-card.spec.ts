import { test, expect } from '../fixtures/game';
import { createMockDeck, createPlayer, drawUntilDuplicate, expectActivePlayer } from '../helpers/game';

test('duplicate card flow eliminates the active player and shows the alert', async ({ loadMockGame }) => {
  const page = await loadMockGame({
    players: [
      createPlayer('Satoshi', { id: 'player-1', hasTurn: true, roundCards: [4] }),
      createPlayer('Ada', { id: 'player-2' }),
      createPlayer('Marcus', { id: 'player-3' }),
      createPlayer('Elena', { id: 'player-4' }),
    ],
    currentTurnPlayerId: 'player-1',
    roundStarterPlayerId: 'player-1',
    deck: createMockDeck([4, 8, 3, 2]),
  });

  await drawUntilDuplicate(page);
  await expect(page.getByTestId('duplicate-alert')).toContainText('Satoshi');
  await expect(page.getByTestId('player-card-satoshi')).toHaveClass(/eliminated/);
  await expectActivePlayer(page, 'Ada');
  await expect(page.locator('[data-active-player="true"]')).not.toContainText('Satoshi');
});
