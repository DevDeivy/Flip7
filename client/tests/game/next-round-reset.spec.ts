import { test, expect } from '../fixtures/game';
import {
  createMockDeck,
  drawCurrentPlayer,
  expectActivePlayer,
  finishRound,
} from '../helpers/game';

test('next round clears round cards but preserves total scores and rotates the starter', async ({ loadMockGame }) => {
  const page = await loadMockGame({
    deck: createMockDeck([4, 8, 9, 1, 6, 7]),
  });

  await drawCurrentPlayer(page);
  await expectActivePlayer(page, 'Ada');
  await expect(page.getByTestId('stand-button')).toBeEnabled();
  await finishRound(page);

  await expect(page.getByTestId('round-summary-modal')).toBeVisible();
  await expect(page.getByTestId('player-card-satoshi')).toContainText('4');
  await page.getByTestId('next-round-button').click();

  await expect(page.locator('.metric-inline.is-primary')).toHaveText('02');
  await expect(page.getByText('0 / 7')).toBeVisible();
  await expect(page.getByText('Roba para comenzar la ronda.')).toBeVisible();
  await expectActivePlayer(page, 'Ada');
  await expect(page.getByTestId('player-card-satoshi')).toContainText('4');
  await expect(page.getByTestId('player-card-ada')).toContainText('0');
});
