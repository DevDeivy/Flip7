import { gameApi } from '../api/gameApi';

export const gameService = {
  getGame: () => gameApi.getGame(),
  startGame: () => gameApi.startGame(),
  drawCard: (playerId: string) => gameApi.drawCard(playerId),
  stand: (playerId: string) => gameApi.stand(playerId),
  nextRound: () => gameApi.nextRound(),
  restart: () => gameApi.restart(),
};
