import {
  advanceToNextRound,
  applyDraw,
  applyStand,
  createInitialGameState,
  restartGame,
} from '../engine/gameEngine';
import type {
  GameActionResultDTO,
  GameResponseDTO,
  GameStateDTO,
  GameTestPlayerConfig,
  GameTestStateConfig,
} from '../types/game';
import { getGameTestConfig } from './gameTestHarness';

const LATENCY_MS = 340;

function getLatencyMs() {
  return getGameTestConfig() ? 0 : LATENCY_MS;
}

function createCard(value: number, index: number) {
  return {
    id: `test-${value}-${index}`,
    value,
  };
}

function normalizePlayers(players: GameTestPlayerConfig[] | undefined) {
  if (!players || players.length === 0) {
    return createInitialGameState().players;
  }

  return players.map((player, index) => ({
    id: player.id ?? `player-${index + 1}`,
    name: player.name,
    totalScore: player.totalScore ?? 0,
    roundCards: (player.roundCards ?? []).map((value, cardIndex) => createCard(value, cardIndex)),
    status: player.status ?? 'playing',
    hasTurn: player.hasTurn ?? index === 0,
  }));
}

function normalizeDeck(deck: number[] | undefined) {
  return (deck ?? []).map((value, index) => createCard(value, index));
}

function normalizeState(config?: GameTestStateConfig): GameStateDTO {
  const baseState = createInitialGameState();

  if (!config) {
    return baseState;
  }

  const players = normalizePlayers(config.players);

  return {
    ...baseState,
    currentRound: config.currentRound ?? baseState.currentRound,
    currentTurnPlayerId: config.currentTurnPlayerId ?? players.find((player) => player.hasTurn)?.id ?? players[0]?.id ?? null,
    roundStarterPlayerId: config.roundStarterPlayerId ?? players.find((player) => player.hasTurn)?.id ?? players[0]?.id ?? null,
    players,
    deck: normalizeDeck(config.deck),
    discard: normalizeDeck(config.discard),
    events: config.events
      ? config.events.map((event, index) => ({
          id: `event-${index}`,
          timestamp: new Date().toISOString(),
          playerId: undefined,
          playerName: undefined,
          title: event.title,
          description: event.description,
          tone: event.tone,
        }))
      : baseState.events,
    gamePhase: config.gamePhase ?? 'playing',
    roundSummary: config.roundSummary ?? null,
    duplicateAlert: config.duplicateAlert ?? null,
    winner: config.winner ?? null,
    riskLevel: config.riskLevel ?? baseState.riskLevel,
  };
}

function delay() {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, getLatencyMs());
  });
}

class MockGameRepository {
  private state: GameStateDTO = normalizeState(getGameTestConfig());

  async getGame(): Promise<GameResponseDTO> {
    await delay();
    return { game: this.state };
  }

  async startGame(): Promise<GameResponseDTO> {
    await delay();
    this.state = normalizeState(getGameTestConfig());
    return { game: this.state };
  }

  async drawCard(playerId: string): Promise<GameActionResultDTO> {
    await delay();
    this.state = applyDraw(this.state, playerId);

    return {
      game: this.state,
      event: this.state.events[0],
    };
  }

  async stand(playerId: string): Promise<GameActionResultDTO> {
    await delay();
    this.state = applyStand(this.state, playerId);

    return {
      game: this.state,
      event: this.state.events[0],
    };
  }

  async nextRound(): Promise<GameResponseDTO> {
    await delay();
    this.state = advanceToNextRound(this.state);
    return { game: this.state };
  }

  async restart(): Promise<GameResponseDTO> {
    await delay();
    const config = getGameTestConfig();

    this.state = config ? normalizeState(config) : restartGame(this.state);
    return { game: this.state };
  }

  async sync(game: GameStateDTO): Promise<GameResponseDTO> {
    await delay();
    this.state = game;
    return { game: this.state };
  }

  setTestState(config: GameTestStateConfig | undefined) {
    this.state = normalizeState(config);
  }
}

export const mockGameRepository = new MockGameRepository();
