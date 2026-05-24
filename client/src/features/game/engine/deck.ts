export const DEFAULT_PLAYER_NAMES = ['Satoshi', 'Ada', 'Marcus', 'Elena'];
export const DEFAULT_GAME_SEED = 314159;

export function createCardId(roundNumber: number, value: number, index: number) {
  return `r${roundNumber}-v${value}-${index}`;
}

function createSeededRandom(seed: number) {
  let value = seed % 2147483647;

  if (value <= 0) {
    value += 2147483646;
  }

  return () => {
    value = (value * 16807) % 2147483647;
    return (value - 1) / 2147483646;
  };
}

function shuffle<T>(items: T[], seed: number) {
  const random = createSeededRandom(seed);
  const result = [...items];

  for (let index = result.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(random() * (index + 1));
    [result[index], result[swapIndex]] = [result[swapIndex], result[index]];
  }

  return result;
}

export function buildDeck(roundNumber: number) {
  const deckValues: number[] = [];

  for (let value = 0; value <= 12; value += 1) {
    const copies = value === 0 ? 1 : value;

    for (let index = 0; index < copies; index += 1) {
      deckValues.push(value);
    }
  }

  const seededDeck = shuffle(deckValues, DEFAULT_GAME_SEED + roundNumber);

  return seededDeck.map((value, index) => ({
    id: createCardId(roundNumber, value, index),
    value,
  }));
}
