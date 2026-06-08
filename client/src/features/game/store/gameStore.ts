import { create } from 'zustand';

import { gameService } from '../services/gameService';
import type { GameEventDTO, GameStateDTO, RoomStateDTO } from '../types/game';

const DEFAULT_PLAYER_NAMES = ['Jugador 1', 'Jugador 2', 'Jugador 3', 'Jugador 4'];

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
