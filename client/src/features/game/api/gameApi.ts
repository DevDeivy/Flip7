import { httpClient } from './httpClient';
import type {
  GameActionResultDTO,
  GameCardDTO,
  GameEventDTO,
  GamePhase,
  GameResponseDTO,
  RoomStateDTO,
  GameStateDTO,
  PlayerDTO,
  PlayerStatus,
  WinnerDTO,
} from '../types/game';

interface BackendPlayerStateDTO {
  playerId: number;
  name: string;
  totalPoints: number;
  roundCards: number[];
  status: 'ACTIVE' | 'STANDING' | 'ELIMINATED';
  hasSecondChance: boolean;
  modifierCardValues: number[];
  roundPoints: number;
}

interface BackendPlayerDTO {
  id: number;
  name: string;
  totalPoints: number;
}

interface BackendGameStateDTO {
  gameId: number;
  status: 'WAITING' | 'PLAYING' | 'FINISHED';
  currentRound: number;
  currentPlayerTurnIndex: number;
  currentPlayerTurnId: number | null;
  startingPlayerIndex: number;
  players: BackendPlayerStateDTO[];
  scoreboard: BackendPlayerDTO[];
  deckRemaining: number;
  lastMessage?: string | null;
  winner?: BackendPlayerDTO | null;
}

interface CreateGameRequest {
  players: string[];
}

interface BackendRoomParticipantDTO {
  id: number;
  name: string;
}

interface BackendRoomStateDTO {
  roomId: number;
  code: string;
  status: 'WAITING' | 'STARTED' | 'CLOSED';
  hostName: string;
  currentPlayers: number;
  minimumPlayersToStart: number;
  gameId: number | null;
  participants: BackendRoomParticipantDTO[];
}

interface CreateRoomRequest {
  hostName: string;
}

interface JoinRoomRequest {
  playerName: string;
}

function mapPlayerStatus(status: BackendPlayerStateDTO['status']): PlayerStatus {
  switch (status) {
    case 'ACTIVE':
      return 'playing';
    case 'STANDING':
      return 'stood';
    case 'ELIMINATED':
      return 'eliminated';
    default:
      return 'playing';
  }
}

function mapGamePhase(status: BackendGameStateDTO['status']): GamePhase {
  switch (status) {
    case 'WAITING':
      return 'idle';
    case 'PLAYING':
      return 'playing';
    case 'FINISHED':
      return 'winner';
    default:
      return 'playing';
  }
}

function mapWinner(winner: BackendPlayerDTO | null | undefined): WinnerDTO | null {
  if (!winner) {
    return null;
  }

  return {
    playerId: String(winner.id),
    playerName: winner.name,
    totalScore: winner.totalPoints,
  };
}

function mapCard(value: number, index: number, playerId: string): GameCardDTO {
  return {
    id: `${playerId}-${value}-${index}`,
    value,
  };
}

function mapPlayer(player: BackendPlayerStateDTO): PlayerDTO {
  const playerId = String(player.playerId);

  return {
    id: playerId,
    name: player.name,
    totalScore: player.totalPoints,
    roundCards: player.roundCards.map((value, index) => mapCard(value, index, playerId)),
    status: mapPlayerStatus(player.status),
    hasTurn: false,
  };
}

function getRiskLevel(players: PlayerDTO[], currentTurnPlayerId: string | null) {
  const activePlayer = players.find((player) => player.id === currentTurnPlayerId) ?? players.find((player) => player.status === 'playing');
  const uniqueCount = activePlayer?.roundCards.length ?? 0;

  return Math.min(100, Math.round((uniqueCount / 7) * 100));
}

function buildEvent(message: string | null | undefined): GameEventDTO[] {
  if (!message) {
    return [];
  }

  return [
    {
      id: `event-${Date.now()}`,
      timestamp: new Date().toISOString(),
      title: 'Última jugada',
      description: message,
      tone: /elimin|duplic|gan|freeze|plant/i.test(message) ? 'warning' : 'primary',
    },
  ];
}

function toClientGameState(state: BackendGameStateDTO): GameStateDTO {
  const players = state.players.map((player, index) => ({
    ...mapPlayer(player),
    hasTurn: state.currentPlayerTurnIndex === index,
  }));

  const deck = Array.from({ length: state.deckRemaining }, (_, index) => ({
    id: `deck-${index}`,
    value: -1,
  }));

  const currentTurnPlayerId = state.currentPlayerTurnId === null ? null : String(state.currentPlayerTurnId);
  const roundStarterPlayerId = state.players[state.startingPlayerIndex] ? String(state.players[state.startingPlayerIndex].playerId) : null;

  return {
    gameId: String(state.gameId),
    gamePhase: mapGamePhase(state.status),
    currentRound: state.currentRound,
    currentTurnPlayerId,
    roundStarterPlayerId,
    players,
    deck,
    discard: [],
    events: buildEvent(state.lastMessage),
    riskLevel: getRiskLevel(players, currentTurnPlayerId),
    roundSummary: null,
    duplicateAlert: null,
    winner: mapWinner(state.winner ?? null),
  };
}

function toClientRoomState(room: BackendRoomStateDTO): RoomStateDTO {
  return {
    roomId: String(room.roomId),
    code: room.code,
    status: room.status,
    hostName: room.hostName,
    currentPlayers: room.currentPlayers,
    minimumPlayersToStart: room.minimumPlayersToStart,
    gameId: room.gameId === null ? null : String(room.gameId),
    participants: room.participants.map((participant) => ({
      id: String(participant.id),
      name: participant.name,
    })),
  };
}

export const gameApi = {
  createGame: async (players: string[]): Promise<GameResponseDTO> => {
    const response = await httpClient.post<BackendGameStateDTO>('/games', { players } satisfies CreateGameRequest);
    return { game: toClientGameState(response.data) };
  },

  getGame: async (gameId: string): Promise<GameResponseDTO> => {
    const response = await httpClient.get<BackendGameStateDTO>(`/games/${gameId}/state`);
    return { game: toClientGameState(response.data) };
  },

  drawCard: async (gameId: string): Promise<GameActionResultDTO> => {
    const response = await httpClient.post<BackendGameStateDTO>(`/games/${gameId}/draw`);
    return { game: toClientGameState(response.data), event: buildEvent(response.data.lastMessage)[0] };
  },

  stand: async (gameId: string): Promise<GameActionResultDTO> => {
    const response = await httpClient.post<BackendGameStateDTO>(`/games/${gameId}/stand`);
    return { game: toClientGameState(response.data), event: buildEvent(response.data.lastMessage)[0] };
  },

  getScoreboard: async (gameId: string): Promise<PlayerDTO[]> => {
    const response = await httpClient.get<BackendPlayerDTO[]>(`/games/${gameId}/scoreboard`);
    return response.data.map((player) => ({
      id: String(player.id),
      name: player.name,
      totalScore: player.totalPoints,
    }));
  },

  getWinner: async (gameId: string): Promise<WinnerDTO | null> => {
    const response = await httpClient.get<BackendPlayerDTO>(`/games/${gameId}/winner`);
    return mapWinner(response.data ?? null);
  },

  createRoom: async (hostName: string): Promise<RoomStateDTO> => {
    const response = await httpClient.post<BackendRoomStateDTO>('/rooms', { hostName } satisfies CreateRoomRequest);
    return toClientRoomState(response.data);
  },

  joinRoom: async (roomCode: string, playerName: string): Promise<RoomStateDTO> => {
    const response = await httpClient.post<BackendRoomStateDTO>(`/rooms/${roomCode}/join`, { playerName } satisfies JoinRoomRequest);
    return toClientRoomState(response.data);
  },

  getRoom: async (roomCode: string): Promise<RoomStateDTO> => {
    const response = await httpClient.get<BackendRoomStateDTO>(`/rooms/${roomCode}`);
    return toClientRoomState(response.data);
  },

  startRoom: async (roomCode: string): Promise<GameResponseDTO> => {
    const response = await httpClient.post<BackendGameStateDTO>(`/rooms/${roomCode}/start`);
    return { game: toClientGameState(response.data) };
  },
};
