import { gameApi } from '../api/gameApi';

export const gameService = {
  createGame: (players: string[]) => gameApi.createGame(players),
  getGame: (gameId: string) => gameApi.getGame(gameId),
  drawCard: (gameId: string) => gameApi.drawCard(gameId),
  stand: (gameId: string) => gameApi.stand(gameId),
  getScoreboard: (gameId: string) => gameApi.getScoreboard(gameId),
  getWinner: (gameId: string) => gameApi.getWinner(gameId),
  createRoom: (hostName: string) => gameApi.createRoom(hostName),
  joinRoom: (roomCode: string, playerName: string) => gameApi.joinRoom(roomCode, playerName),
  getRoom: (roomCode: string) => gameApi.getRoom(roomCode),
  startRoom: (roomCode: string) => gameApi.startRoom(roomCode),
};
