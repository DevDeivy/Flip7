import { mockGameRepository } from '../mocks/mockGameRepository';

export const gameApi = {
  getGame: () => mockGameRepository.getGame(),
  startGame: () => mockGameRepository.startGame(),
  drawCard: (playerId: string) => mockGameRepository.drawCard(playerId),
  stand: (playerId: string) => mockGameRepository.stand(playerId),
  nextRound: () => mockGameRepository.nextRound(),
  restart: () => mockGameRepository.restart(),
};
