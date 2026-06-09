import { create } from 'zustand';

import { gameService } from '../services/gameService';
import type {
  GameEventDTO,
  GameStateDTO,
  GameTestStateConfig,
  PlayerDTO,
  RoomStateDTO,
  RoundSummaryDTO,
} from '../types/game';

const DEFAULT_PLAYER_NAMES = ['Jugador 1', 'Jugador 2', 'Jugador 3', 'Jugador 4'];
const TEST_GAME_ID = 'test-game-local';

declare global {
  interface Window {
    __FLIP7_TEST__?: {
      state?: GameTestStateConfig;
    };
  }
}

function nowIso() {
  return new Date().toISOString();
}

function sumRoundPoints(player: PlayerDTO) {
  return player.roundCards.reduce((total, card) => total + card.value, 0);
}

function createTestCard(playerId: string, value: number, index: number) {
  return {
    id: `test-${playerId}-${value}-${index}`,
    value,
  };
}

function playerRiskLevel(game: GameStateDTO) {
  const active = game.players.find((player) => player.id === game.currentTurnPlayerId);
  const cardCount = active?.roundCards.length ?? 0;
  return Math.min(100, Math.round((cardCount / 7) * 100));
}

function nextPlayingIndex(players: PlayerDTO[], fromIndex: number): number | null {
  if (players.length === 0) {
    return null;
  }

  for (let offset = 1; offset <= players.length; offset += 1) {
    const index = (fromIndex + offset) % players.length;
    if (players[index].status === 'playing') {
      return index;
    }
  }

  return null;
}

function clearTurns(players: PlayerDTO[]) {
  return players.map((player) => ({ ...player, hasTurn: false }));
}

function setTurn(players: PlayerDTO[], index: number | null) {
  return players.map((player, playerIndex) => ({
    ...player,
    hasTurn: index !== null && playerIndex === index,
  }));
}

function appendTestEvent(game: GameStateDTO, description: string, playerName?: string, tone: GameEventDTO['tone'] = 'primary') {
  const event: GameEventDTO = {
    id: `event-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    title: 'Jugada',
    description,
    playerName,
    tone,
    timestamp: nowIso(),
  };

  return {
    ...game,
    events: [event, ...game.events].slice(0, 40),
  };
}

function buildRoundSummary(game: GameStateDTO, endingReason: RoundSummaryDTO['endingReason']) {
  const scoredPlayers = game.players.map((player) => {
    if (player.status === 'eliminated') {
      return player;
    }

    return {
      ...player,
      totalScore: player.totalScore + sumRoundPoints(player),
    };
  });

  return {
    ...game,
    gamePhase: 'roundSummary' as const,
    currentTurnPlayerId: null,
    players: clearTurns(scoredPlayers),
    roundSummary: {
      round: game.currentRound,
      endingReason,
      players: scoredPlayers.map((player) => ({
        playerId: player.id,
        playerName: player.name,
        roundScore: sumRoundPoints(player),
        totalScore: player.totalScore,
        status: player.status,
        cards: player.roundCards.map((card) => card.value),
        bonus: 0,
      })),
    },
    duplicateAlert: null,
  };
}

function resolveRoundEnd(game: GameStateDTO): RoundSummaryDTO['endingReason'] | null {
  if (game.players.some((player) => player.status === 'playing' && player.roundCards.length >= 7)) {
    return 'seven-cards';
  }

  const playingCount = game.players.filter((player) => player.status === 'playing').length;
  if (playingCount === 0) {
    const allEliminated = game.players.every((player) => player.status === 'eliminated');
    return allEliminated ? 'all-eliminated' : 'all-stand';
  }

  if (game.deck.length === 0) {
    return 'deck-empty';
  }

  return null;
}

function toTestGameState(config: GameTestStateConfig): GameStateDTO {
  const players = (config.players ?? DEFAULT_PLAYER_NAMES.map((name) => ({ name }))).map((player, index) => {
    const id = player.id ?? `player-${index + 1}`;
    const roundCards = (player.roundCards ?? []).map((value, cardIndex) => createTestCard(id, value, cardIndex));
    const specialCards = (player.specialCards ?? []).map((value, cardIndex) => createTestCard(id, value, cardIndex));
    const baseScore = player.totalScore ?? 0;
    const roundScore = roundCards.reduce((total, card) => total + card.value, 0);

    return {
      id,
      name: player.name,
      totalScore: baseScore,
      aiControlled: false,
      roundCards,
      specialCards,
      hasSecondChance: player.hasSecondChance ?? false,
      status: player.status ?? 'playing',
      hasTurn: Boolean(player.hasTurn),
    };
  });

  const currentTurnPlayerId = config.currentTurnPlayerId
    ?? players.find((player) => player.hasTurn)?.id
    ?? players.find((player) => player.status === 'playing')?.id
    ?? null;

  const deck = (config.deck ?? []).map((value, index) => createTestCard('deck', value, index));
  const discard = (config.discard ?? []).map((value, index) => createTestCard('discard', value, index));

  const events = (config.events ?? []).map((event, index) => ({
    id: `seed-event-${index}`,
    title: event.title,
    description: event.description,
    tone: event.tone,
    timestamp: nowIso(),
  }));

  const startedPlayers = players.map((player) => ({
    ...player,
    hasTurn: player.id === currentTurnPlayerId,
  }));

  return {
    gameId: TEST_GAME_ID,
    gamePhase: config.gamePhase ?? 'playing',
    currentRound: config.currentRound ?? 1,
    currentTurnPlayerId,
    roundStarterPlayerId: config.roundStarterPlayerId ?? currentTurnPlayerId,
    players: startedPlayers,
    deck,
    discard,
    events,
    riskLevel: config.riskLevel ?? 0,
    roundSummary: config.roundSummary ?? null,
    duplicateAlert: config.duplicateAlert ?? null,
    winner: config.winner ?? null,
  };
}

function playTestDraw(game: GameStateDTO): GameStateDTO {
  if (game.gamePhase !== 'playing' || !game.currentTurnPlayerId) {
    return game;
  }

  const currentIndex = game.players.findIndex((player) => player.id === game.currentTurnPlayerId);
  if (currentIndex < 0) {
    return game;
  }

  const drawn = game.deck[0];
  if (!drawn) {
    return buildRoundSummary(game, 'deck-empty');
  }

  const deck = game.deck.slice(1);
  const players = game.players.map((player) => ({
    ...player,
    roundCards: [...player.roundCards],
    specialCards: [...player.specialCards],
  }));

  const currentPlayer = players[currentIndex];
  const duplicate = currentPlayer.roundCards.some((card) => card.value === drawn.value);

  let nextGame: GameStateDTO = {
    ...game,
    deck,
    players,
    discard: [drawn, ...game.discard],
    duplicateAlert: null,
  };

  if (duplicate) {
    const baseline = currentPlayer.totalScore - sumRoundPoints(currentPlayer);
    players[currentIndex] = {
      ...currentPlayer,
      status: 'eliminated',
      totalScore: Math.max(0, baseline),
      roundCards: [],
    };

    nextGame = appendTestEvent(nextGame, `Carta repetida. ${currentPlayer.name} ha sido eliminado.`, currentPlayer.name, 'warning');
    nextGame = {
      ...nextGame,
      duplicateAlert: {
        playerId: currentPlayer.id,
        playerName: currentPlayer.name,
        cardValue: drawn.value,
        message: `${currentPlayer.name} fue eliminado por duplicar ${drawn.value}.`,
      },
    };
  } else {
    players[currentIndex] = {
      ...currentPlayer,
      roundCards: [...currentPlayer.roundCards, drawn],
    };
    nextGame = appendTestEvent(nextGame, `${currentPlayer.name} robó ${drawn.value}`, currentPlayer.name);
  }

  const winner = players.find((player) => player.totalScore + sumRoundPoints(player) >= 200);
  if (winner) {
    const winnerTotal = winner.totalScore + sumRoundPoints(winner);

    return {
      ...nextGame,
      gamePhase: 'winner',
      winner: {
        playerId: winner.id,
        playerName: winner.name,
        totalScore: winnerTotal,
      },
      currentTurnPlayerId: null,
      players: clearTurns(players).map((player) => (
        player.id === winner.id
          ? { ...player, totalScore: winnerTotal }
          : player
      )),
      riskLevel: 0,
    };
  }

  const endingReason = resolveRoundEnd({ ...nextGame, players });
  if (endingReason) {
    const ended = buildRoundSummary({ ...nextGame, players }, endingReason);
    return appendTestEvent(ended, `${currentPlayer.name} aseguró sus puntos de ronda.`, currentPlayer.name);
  }

  const nextIndex = nextPlayingIndex(players, currentIndex);
  const rotatedPlayers = setTurn(players, nextIndex);
  return {
    ...nextGame,
    players: rotatedPlayers,
    currentTurnPlayerId: nextIndex === null ? null : rotatedPlayers[nextIndex].id,
    riskLevel: playerRiskLevel({ ...nextGame, players: rotatedPlayers, currentTurnPlayerId: nextIndex === null ? null : rotatedPlayers[nextIndex].id }),
  };
}

function playTestStand(game: GameStateDTO): GameStateDTO {
  if (game.gamePhase !== 'playing' || !game.currentTurnPlayerId) {
    return game;
  }

  const currentIndex = game.players.findIndex((player) => player.id === game.currentTurnPlayerId);
  if (currentIndex < 0) {
    return game;
  }

  const players = game.players.map((player) => ({ ...player }));
  const currentPlayer = players[currentIndex];
  players[currentIndex] = {
    ...currentPlayer,
    status: 'stood',
  };

  let nextGame = appendTestEvent(
    { ...game, players, duplicateAlert: null },
    `${currentPlayer.name} se ha plantado con ${sumRoundPoints(currentPlayer)} puntos.`,
    currentPlayer.name,
    'warning',
  );

  const endingReason = resolveRoundEnd(nextGame);
  if (endingReason) {
    const ended = buildRoundSummary(nextGame, endingReason);
    return appendTestEvent(ended, `${currentPlayer.name} aseguró sus puntos de ronda.`, currentPlayer.name);
  }

  const nextIndex = nextPlayingIndex(players, currentIndex);
  const rotatedPlayers = setTurn(players, nextIndex);
  nextGame = {
    ...nextGame,
    players: rotatedPlayers,
    currentTurnPlayerId: nextIndex === null ? null : rotatedPlayers[nextIndex].id,
    riskLevel: playerRiskLevel({ ...nextGame, players: rotatedPlayers, currentTurnPlayerId: nextIndex === null ? null : rotatedPlayers[nextIndex].id }),
  };

  return nextGame;
}

function playTestNextRound(game: GameStateDTO): GameStateDTO {
  if (game.gamePhase !== 'roundSummary') {
    return game;
  }

  const starterIndex = game.roundStarterPlayerId
    ? game.players.findIndex((player) => player.id === game.roundStarterPlayerId)
    : -1;
  const nextStarterIndex = game.players.length > 0 ? (starterIndex + 1 + game.players.length) % game.players.length : 0;

  const players = game.players.map((player) => ({
    ...player,
    roundCards: [],
    specialCards: [],
    status: 'playing' as const,
    hasTurn: false,
  }));

  const startedPlayers = setTurn(players, nextStarterIndex);
  const nextStarterId = startedPlayers[nextStarterIndex]?.id ?? null;

  return {
    ...game,
    gamePhase: 'playing',
    currentRound: game.currentRound + 1,
    currentTurnPlayerId: nextStarterId,
    roundStarterPlayerId: nextStarterId,
    players: startedPlayers,
    duplicateAlert: null,
    roundSummary: null,
    riskLevel: 0,
  };
}

function getTestStateConfig() {
  if (typeof window === 'undefined') {
    return undefined;
  }

  return window.__FLIP7_TEST__?.state;
}

function mergeEvents(previousEvents: GameEventDTO[], incomingEvents: GameEventDTO[]): GameEventDTO[] {
  if (incomingEvents.length === 0) {
    return previousEvents;
  }

  const merged = [...incomingEvents, ...previousEvents];
  const deduped = merged.filter((event, index, source) => {
    if (index === 0) {
      return true;
    }

    const previous = source[index - 1];
    return !(previous.description === event.description && previous.playerName === event.playerName && previous.tone === event.tone);
  });

  return deduped.slice(0, 40);
}

function mergeGameState(previousGame: GameStateDTO | null, incomingGame: GameStateDTO): GameStateDTO {
  return {
    ...incomingGame,
    events: mergeEvents(previousGame?.events ?? [], incomingGame.events),
  };
}

interface GameStoreState {
  game: GameStateDTO | null;
  room: RoomStateDTO | null;
  isBusy: boolean;
  error: string | null;
  hasInitialized: boolean;
  lastPlayers: string[];
  playerAlias: string;
  bootstrapTestGame: () => void;
  createGame: (players: string[]) => Promise<void>;
  initializeAiGame: () => Promise<void>;
  initializeGame: () => Promise<void>;
  createRoom: (hostName: string) => Promise<void>;
  joinRoom: (roomCode: string, playerName: string) => Promise<void>;
  refreshRoom: () => Promise<void>;
  startRoom: () => Promise<void>;
  drawCard: (_playerId?: string) => Promise<void>;
  stand: (_playerId?: string) => Promise<void>;
  nextRound: () => Promise<void>;
  restartGame: () => Promise<void>;
  dismissDuplicateAlert: () => void;
}

async function withGuard<T>(handler: () => Promise<T>, onError: (message: string) => void): Promise<T | undefined> {
  try {
    return await handler();
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unexpected game service failure';
    onError(message);
    return undefined;
  }
}

export const useGameStore = create<GameStoreState>((set, get) => ({
  game: null,
  room: null,
  isBusy: false,
  error: null,
  hasInitialized: false,
  lastPlayers: DEFAULT_PLAYER_NAMES,
  playerAlias: '',

  bootstrapTestGame: () => {
    if (get().game || get().room) {
      return;
    }

    const config = getTestStateConfig();
    if (!config) {
      return;
    }

    const game = toTestGameState(config);
    set({
      game,
      room: null,
      hasInitialized: true,
      playerAlias: game.players[0]?.name ?? '',
      error: null,
    });
  },

  createGame: async (players: string[]) => {
    const normalizedPlayers = players.map((player) => player.trim()).filter(Boolean);

    if (normalizedPlayers.length < 4 || normalizedPlayers.length > 8) {
      set({ error: 'Debes registrar entre 4 y 8 jugadores.' });
      return;
    }

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const response = await gameService.createGame(normalizedPlayers);
      set((state) => ({
        game: mergeGameState(state.game, response.game),
        room: null,
        hasInitialized: true,
        lastPlayers: normalizedPlayers,
      }));
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  initializeAiGame: async () => {
    if (get().hasInitialized || get().isBusy) {
      return;
    }

    const alias = get().playerAlias.trim() || 'Jugador';

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const response = await gameService.createAiGame(alias);
      set((state) => ({
        game: mergeGameState(state.game, response.game),
        room: null,
        hasInitialized: true,
        lastPlayers: [alias, 'FLIP7 AI'],
        playerAlias: alias,
      }));
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  initializeGame: async () => {
    if (get().hasInitialized || get().isBusy) {
      return;
    }

    await get().createGame(DEFAULT_PLAYER_NAMES);
  },

  createRoom: async (hostName: string) => {
    const alias = hostName.trim();
    if (!alias) {
      set({ error: 'Debes ingresar tu nombre para crear una sala.' });
      return;
    }

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const room = await gameService.createRoom(alias);
      set({
        room,
        game: null,
        playerAlias: alias,
        hasInitialized: true,
      });
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  joinRoom: async (roomCode: string, playerName: string) => {
    const code = roomCode.trim().toUpperCase();
    const alias = playerName.trim();

    if (!code || !alias) {
      set({ error: 'Debes ingresar código de sala y nombre para unirte.' });
      return;
    }

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const room = await gameService.joinRoom(code, alias);
      set({
        room,
        game: null,
        playerAlias: alias,
        hasInitialized: true,
      });
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  refreshRoom: async () => {
    const room = get().room;
    if (!room) {
      return;
    }

    await withGuard(async () => {
      const refreshed = await gameService.getRoom(room.code);
      if (refreshed.status === 'STARTED' && refreshed.gameId) {
        const state = await gameService.getGame(refreshed.gameId);
        set((current) => ({ game: mergeGameState(current.game, state.game), room: refreshed }));
        return;
      }

      set({ room: refreshed });
    }, (message) => set({ error: message }));
  },

  startRoom: async () => {
    const room = get().room;
    if (!room) {
      return;
    }

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const response = await gameService.startRoom(room.code);
      const refreshed = await gameService.getRoom(room.code);
      set((state) => ({ game: mergeGameState(state.game, response.game), room: refreshed }));
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  drawCard: async (playerId?: string) => {
    const game = get().game;

    if (!game) {
      return;
    }

    const targetPlayerId = playerId ?? game.currentTurnPlayerId;

    if (!targetPlayerId) {
      return;
    }

    if (game.gameId === TEST_GAME_ID) {
      set((state) => (state.game ? { game: playTestDraw(state.game) } : state));
      return;
    }

    set({ isBusy: true, error: null });

    const pollForAiTurn = (gameId: string) => {
      const poll = async () => {
        try {
          const refreshed = await gameService.getGame(gameId);
          
          set((state) => {
            if (state.game?.gameId !== gameId) return state;
            return { game: mergeGameState(state.game, refreshed.game) };
          });

          const nextTurnPlayer = refreshed.game.players.find((player) => player.hasTurn);
          const isAiNext = nextTurnPlayer?.aiControlled;
          const isRoundStillActive = refreshed.game.gamePhase === 'playing';

          const currentState = get();

          // Seguimos haciendo polling si es turno de la IA O si la ronda cambió pero el cliente aún no se enteró
          if (isRoundStillActive && isAiNext) {
            window.setTimeout(poll, 1200);
          } else if (currentState.game && refreshed.game.currentRound !== currentState.game.currentRound) {
            // Si la ronda cambió, forzamos una actualización final para que el jugador vea su turno
            set({ game: mergeGameState(currentState.game, refreshed.game) });
          }
        } catch (err) {
          console.error('Polling error:', err);
          window.setTimeout(poll, 2000);
        }
      };

      window.setTimeout(poll, 1200);
    };

    await withGuard(async () => {
      const response = await gameService.drawCard(game.gameId);
      set((state) => ({ game: mergeGameState(state.game, response.game) }));

      const currentTurnPlayer = response.game.players.find((player) => player.hasTurn);
      if (response.game.gamePhase === 'playing' && currentTurnPlayer?.aiControlled) {
        pollForAiTurn(response.game.gameId);
      }
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  stand: async (playerId?: string) => {
    const game = get().game;

    if (!game) {
      return;
    }

    const targetPlayerId = playerId ?? game.currentTurnPlayerId;

    if (!targetPlayerId) {
      return;
    }

    if (game.gameId === TEST_GAME_ID) {
      set((state) => (state.game ? { game: playTestStand(state.game) } : state));
      return;
    }

    set({ isBusy: true, error: null });

    const pollForAiTurn = (gameId: string) => {
      window.setTimeout(async () => {
        try {
          const refreshed = await gameService.getGame(gameId);

          set((state) => {
            if (state.game?.gameId !== gameId) {
              return state;
            }

            return { game: mergeGameState(state.game, refreshed.game) };
          });

          const nextTurnPlayer = refreshed.game.players.find((player) => player.hasTurn);
          if (refreshed.game.gamePhase === 'playing' && nextTurnPlayer?.aiControlled) {
            pollForAiTurn(gameId);
          }
        } catch {
          pollForAiTurn(gameId);
        }
      }, 1200);
    };

    await withGuard(async () => {
      const response = await gameService.stand(game.gameId);
      set((state) => ({ game: mergeGameState(state.game, response.game) }));

      const currentTurnPlayer = response.game.players.find((player) => player.hasTurn);
      if (response.game.gamePhase === 'playing' && currentTurnPlayer?.aiControlled) {
        pollForAiTurn(response.game.gameId);
      }
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  nextRound: async () => {
    const game = get().game;

    if (!game) {
      return;
    }

    if (game.gameId === TEST_GAME_ID) {
      set((state) => (state.game ? { game: playTestNextRound(state.game) } : state));
      return;
    }

    set({ isBusy: true, error: null });

    await withGuard(async () => {
      const response = await gameService.getGame(game.gameId);
      set((state) => ({ game: mergeGameState(state.game, response.game) }));
    }, (message) => set({ error: message }))
      .finally(() => set({ isBusy: false }));
  },

  restartGame: async () => {
    set({
      game: null,
      room: null,
      hasInitialized: false,
      playerAlias: '',
      error: null,
    });
  },

  dismissDuplicateAlert: () => {
    const game = get().game;

    if (!game) {
      return;
    }

    set({
      game: {
        ...game,
        duplicateAlert: null,
      },
    });
  },
}));
