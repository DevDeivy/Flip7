import { test, expect } from '../fixtures/game';
import { createMockDeck, createPlayer, drawCurrentPlayer } from '../helpers/game';

test('winner flow locks the table when a player reaches 200 points', async ({ loadMockGame }) => {
  const page = await loadMockGame({
    players: [
      createPlayer('Satoshi', { id: 'player-1', hasTurn: true, totalScore: 195 }),
      createPlayer('Ada', { id: 'player-2', totalScore: 84 }),
      createPlayer('Marcus', { id: 'player-3', totalScore: 61 }),
      createPlayer('Elena', { id: 'player-4', totalScore: 42 }),
    ],
    currentTurnPlayerId: 'player-1',
    roundStarterPlayerId: 'player-1',
    currentRound: 7,
    deck: createMockDeck([5, 4, 3, 2]),
  });

  await drawCurrentPlayer(page);

  await expect(page.getByTestId('winner-modal')).toBeVisible();
  await expect(page.getByTestId('winner-modal')).toContainText('Satoshi');
  await expect(page.getByTestId('winner-modal')).toContainText('200');
  await expect(page.getByTestId('draw-button')).toBeDisabled();
  await expect(page.getByTestId('stand-button')).toBeDisabled();
  await expect(page.getByTestId('player-card-satoshi')).toContainText('200');
});
