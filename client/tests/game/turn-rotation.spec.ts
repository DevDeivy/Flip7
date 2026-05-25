import { test, expect } from '../fixtures/game';
import {
  createMockDeck,
  createPlayer,
  drawCurrentPlayer,
  expectActivePlayer,
} from '../helpers/game';

test('turn rotation skips stood and eliminated players', async ({ loadMockGame }) => {
  const page = await loadMockGame({
    players: [
      createPlayer('Satoshi', { id: 'player-1', status: 'stood', roundCards: [1], totalScore: 12 }),
      createPlayer('Ada', { id: 'player-2', status: 'eliminated', roundCards: [2], totalScore: 8 }),
      createPlayer('Marcus', { id: 'player-3', hasTurn: true, roundCards: [3], totalScore: 5 }),
      createPlayer('Elena', { id: 'player-4', totalScore: 1 }),
    ],
    currentTurnPlayerId: 'player-3',
    roundStarterPlayerId: 'player-3',
    deck: createMockDeck([5, 6, 7]),
  });

  await expectActivePlayer(page, 'Marcus');
  await drawCurrentPlayer(page);
  await expectActivePlayer(page, 'Elena');

  await drawCurrentPlayer(page);
  await expectActivePlayer(page, 'Marcus');

  await expect(page.getByTestId('player-card-satoshi')).toHaveClass(/stood/);
  await expect(page.getByTestId('player-card-ada')).toHaveClass(/eliminated/);
  await expect(page.getByTestId('player-card-marcus')).not.toHaveClass(/eliminated/);
});
