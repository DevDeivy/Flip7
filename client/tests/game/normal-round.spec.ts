import { test, expect } from '../fixtures/game';
import { createMockDeck, drawCurrentPlayer, expectActivePlayer, finishRound } from '../helpers/game';

test('normal round flow rotates turns and opens the round summary', async ({ loadMockGame }) => {
  const page = await loadMockGame({ deck: createMockDeck([2, 5, 9, 1, 8, 3, 6, 4]) });

  await expectActivePlayer(page, 'Satoshi');

  await drawCurrentPlayer(page);
  await expectActivePlayer(page, 'Ada');
  await expect(page.getByTestId('event-log')).toContainText('robó 2');

  await drawCurrentPlayer(page);
  await expectActivePlayer(page, 'Marcus');
  await expect(page.getByTestId('event-log')).toContainText('robó 5');

  await drawCurrentPlayer(page);
  await expectActivePlayer(page, 'Elena');

  await drawCurrentPlayer(page);
  await expectActivePlayer(page, 'Satoshi');
  await expect(page.getByTestId('stand-button')).toBeEnabled();

  await finishRound(page);

  await expect(page.getByTestId('round-summary-modal')).toBeVisible();
  await expect(page.getByTestId('round-summary-modal')).toContainText('Resumen de ronda');
  await expect(page.getByTestId('round-summary-modal')).toContainText('Satoshi');
  await expect(page.getByTestId('round-summary-modal')).toContainText('Ada');
  await expect(page.getByTestId('event-log')).toContainText('aseguró');
});
