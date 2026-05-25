import { test, expect } from '../fixtures/game';
import {
  acknowledgeDuplicateAlert,
  createMockDeck,
  createPlayer,
  drawCurrentPlayer,
  expectActivePlayer,
} from '../helpers/game';

test('all players are eliminated and the round closes automatically', async ({ loadMockGame }) => {
  const page = await loadMockGame({
    players: [
      createPlayer('Satoshi', { id: 'player-1', hasTurn: true, roundCards: [4] }),
      createPlayer('Ada', { id: 'player-2', roundCards: [7] }),
      createPlayer('Marcus', { id: 'player-3', roundCards: [9] }),
      createPlayer('Elena', { id: 'player-4', roundCards: [2] }),
    ],
    currentTurnPlayerId: 'player-1',
    roundStarterPlayerId: 'player-1',
    deck: createMockDeck([4, 7, 9, 2]),
  });

  await drawCurrentPlayer(page);
  await expect(page.getByTestId('duplicate-alert')).toBeVisible();
  await expect(page.getByTestId('duplicate-alert')).toContainText('Satoshi');
  await expect(page.getByTestId('player-card-satoshi')).toHaveClass(/eliminated/);
  await acknowledgeDuplicateAlert(page);
  await expectActivePlayer(page, 'Ada');

  await drawCurrentPlayer(page);
  await expect(page.getByTestId('duplicate-alert')).toBeVisible();
  await expect(page.getByTestId('player-card-ada')).toHaveClass(/eliminated/);
  await acknowledgeDuplicateAlert(page);
  await expectActivePlayer(page, 'Marcus');

  await drawCurrentPlayer(page);
  await expect(page.getByTestId('duplicate-alert')).toBeVisible();
  await expect(page.getByTestId('player-card-marcus')).toHaveClass(/eliminated/);
  await acknowledgeDuplicateAlert(page);
  await expectActivePlayer(page, 'Elena');

  await drawCurrentPlayer(page);

  await expect(page.getByTestId('round-summary-modal')).toBeVisible();
  await expect(page.getByTestId('round-summary-modal')).toContainText('todos eliminados');
  await expect(page.getByTestId('player-card-satoshi')).toContainText('0');
  await expect(page.getByTestId('player-card-ada')).toContainText('0');
  await expect(page.getByTestId('player-card-marcus')).toContainText('0');
  await expect(page.getByTestId('player-card-elena')).toContainText('0');
});
