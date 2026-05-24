import {
  DEFAULT_PLAYER_NAMES,
  buildDeck,
} from './deck';
import type {
  DuplicateAlertDTO,
  GameCardDTO,
  GameEventDTO,
  GameStateDTO,
  PlayerDTO,
  PlayerStatus,
  RoundSummaryDTO,
  RoundSummaryPlayerDTO,
  WinnerDTO,
} from '../types/game';

const WINNING_SCORE = 200;
const SEVEN_CARD_BONUS = 15;

function createPlayer(id: string, name: string): PlayerDTO {
  return {
    id,
    name,
    totalScore: 0,
    roundCards: [],
    status: 'playing',
    hasTurn: false,
  };
}

function createEvent(event: Omit<GameEventDTO, 'id' | 'timestamp'>): GameEventDTO {
  return {
    ...event,
    id: crypto.randomUUID(),
    timestamp: new Date().toISOString(),
  };
}

function cloneCard(card: GameCardDTO): GameCardDTO {
  return { ...card };
}

function clonePlayers(players: PlayerDTO[]): PlayerDTO[] {
  return players.map((player) => ({
    ...player,
    roundCards: player.roundCards.map(cloneCard),
  }));
}

function getActivePlayers(players: PlayerDTO[]) {
  return players.filter((player) => player.status === 'playing');
}

function getPlayerIndex(players: PlayerDTO[], playerId: string) {
  return players.findIndex((player) => player.id === playerId);
}

function getNextPlayerId(players: PlayerDTO[], currentPlayerId: string | null) {
  const activePlayers = getActivePlayers(players);

  if (activePlayers.length === 0) {
    return null;
  }

  const currentIndex = currentPlayerId ? getPlayerIndex(players, currentPlayerId) : -1;

  for (let offset = 1; offset <= players.length; offset += 1) {
    const candidate = players[(currentIndex + offset) % players.length];

    if (candidate.status === 'playing') {
      return candidate.id;
    }
  }

  return activePlayers[0]?.id ?? null;
}

function getRoundScore(player: PlayerDTO) {
  const cardValues = player.roundCards.map((card) => card.value);
  const sum = cardValues.reduce((total, value) => total + value, 0);
  const bonus = cardValues.length === 7 ? SEVEN_CARD_BONUS : 0;

  return {
    sum,
    bonus,
    roundScore: sum + bonus,
  };
}

function isRoundFinished(players: PlayerDTO[], deck: GameCardDTO[]) {
  if (deck.length === 0) {
    return true;
  }

  const hasSevenCards = players.some((player) => player.roundCards.length >= 7);

  if (hasSevenCards) {
    return true;
  }

  const activePlayers = getActivePlayers(players);

  return activePlayers.length === 0;
}

function determineEndingReason(players: PlayerDTO[], deck: GameCardDTO[]): RoundSummaryDTO['endingReason'] {
  const activePlayers = getActivePlayers(players);

  if (activePlayers.length === 0) {
    return players.every((player) => player.status === 'eliminated') ? 'all-eliminated' : 'all-stand';
  }

  if (deck.length === 0) {
    return 'deck-empty';
  }

  if (players.some((player) => player.roundCards.length >= 7)) {
    return 'seven-cards';
  }

  return 'all-stand';
}

function createRoundSummary(players: PlayerDTO[], deck: GameCardDTO[], round: number): RoundSummaryDTO {
  return {
    round,
    endingReason: determineEndingReason(players, deck),
    players: players.map((player): RoundSummaryPlayerDTO => {
      const { roundScore, bonus } = getRoundScore(player);

      return {
        playerId: player.id,
        playerName: player.name,
        roundScore: player.status === 'eliminated' ? 0 : roundScore,
        totalScore: player.totalScore,
        status: player.status,
        cards: player.roundCards.map((card) => card.value),
        bonus: player.status === 'eliminated' ? 0 : bonus,
      };
    }),
  };
}

function applyRoundScores(players: PlayerDTO[]) {
  return players.map((player) => {
    if (player.status === 'eliminated') {
      return player;
    }

    const { roundScore } = getRoundScore(player);

    return {
      ...player,
      totalScore: player.totalScore + roundScore,
    };
  });
}

function findWinner(players: PlayerDTO[]): WinnerDTO | null {
  const sortedPlayers = [...players].sort((left, right) => right.totalScore - left.totalScore);
  const winner = sortedPlayers[0];

  if (!winner || winner.totalScore < WINNING_SCORE) {
    return null;
  }

  return {
    playerId: winner.id,
    playerName: winner.name,
    totalScore: winner.totalScore,
  };
}

function buildBaseState(round = 1, players = DEFAULT_PLAYER_NAMES): GameStateDTO {
  const playerModels = players.map((name, index) => createPlayer(`player-${index + 1}`, name));
  const firstPlayer = playerModels[0]?.id ?? null;

  return {
    gameId: crypto.randomUUID(),
    gamePhase: 'playing',
    currentRound: round,
    currentTurnPlayerId: firstPlayer,
    roundStarterPlayerId: firstPlayer,
    players: playerModels.map((player, index) => ({
      ...player,
      hasTurn: index === 0,
    })),
    deck: buildDeck(round),
    discard: [],
    events: [
      createEvent({
        title: 'Game started',
        description: 'The table is ready and the first round is live.',
        tone: 'primary',
      }),
    ],
    riskLevel: 0,
    roundSummary: null,
    duplicateAlert: null,
    winner: null,
  };
}

function setActiveTurn(players: PlayerDTO[], currentTurnPlayerId: string | null) {
  return players.map((player) => ({
    ...player,
    hasTurn: player.id === currentTurnPlayerId,
  }));
}

function updateRiskLevel(players: PlayerDTO[]) {
  const activePlayer = players.find((player) => player.hasTurn) ?? players.find((player) => player.status === 'playing');
  const uniqueCount = activePlayer?.roundCards.length ?? 0;

  return Math.min(100, Math.round((uniqueCount / 7) * 100));
}

export function createInitialGameState() {
  const state = buildBaseState();

  return {
    ...state,
    riskLevel: updateRiskLevel(state.players),
  };
}

function finalizeRound(state: GameStateDTO): GameStateDTO {
  const scoredPlayers = applyRoundScores(state.players);
  const winner = findWinner(scoredPlayers);
  const summary = createRoundSummary(scoredPlayers, state.deck, state.currentRound);
  const roundEvent = createEvent({
    title: 'Ronda completada',
    description: `La ronda ${state.currentRound} terminó por ${summary.endingReason.replace('-', ' ')}.`,
    tone: 'warning',
  });

  return {
    ...state,
    players: scoredPlayers,
    currentTurnPlayerId: null,
    gamePhase: winner ? 'winner' : 'roundSummary',
    events: [roundEvent, ...state.events],
    roundSummary: summary,
    duplicateAlert: null,
    winner,
    riskLevel: 0,
  };
}

function finalizeWinner(state: GameStateDTO, winner: WinnerDTO): GameStateDTO {
  const scoredPlayers = applyRoundScores(state.players);

  return {
    ...state,
    players: scoredPlayers,
    currentTurnPlayerId: null,
    gamePhase: 'winner',
    events: [
      createEvent({
        title: 'Partida ganada',
        description: `${winner.playerName} alcanzó ${winner.totalScore} puntos y ganó la partida.`,
        playerId: winner.playerId,
        playerName: winner.playerName,
        value: winner.totalScore,
        tone: 'primary',
      }),
      ...state.events,
    ],
    roundSummary: createRoundSummary(scoredPlayers, state.deck, state.currentRound),
    duplicateAlert: null,
    winner,
    riskLevel: 0,
  };
}

function findWinnerFromState(players: PlayerDTO[]) {
  const scoredPlayers = applyRoundScores(players);
  return findWinner(scoredPlayers);
}

function buildDuplicateAlert(player: PlayerDTO, cardValue: number): DuplicateAlertDTO {
  return {
    playerId: player.id,
    playerName: player.name,
    cardValue,
    message: `${player.name} robó una carta duplicada de ${cardValue} y quedó eliminado de la ronda.`,
  };
}

export function applyDraw(state: GameStateDTO, playerId: string) {
  if (state.gamePhase !== 'playing') {
    return state;
  }

  if (state.currentTurnPlayerId !== playerId) {
    return state;
  }

  if (state.deck.length === 0) {
    return finalizeRound(state);
  }

  const [nextCard, ...nextDeck] = state.deck;
  const players = clonePlayers(state.players);
  const playerIndex = getPlayerIndex(players, playerId);
  const player = players[playerIndex];

  if (!player) {
    return state;
  }

  const duplicate = player.roundCards.some((card) => card.value === nextCard.value);
  const eventBase = duplicate
    ? createEvent({
        title: 'Duplicado',
        description: `${player.name} robó un duplicado ${nextCard.value} y quedó fuera de la ronda.`,
        playerId: player.id,
        playerName: player.name,
        value: nextCard.value,
        tone: 'danger',
      })
    : createEvent({
        title: 'Carta robada',
        description: `${player.name} robó ${nextCard.value}.`,
        playerId: player.id,
        playerName: player.name,
        value: nextCard.value,
        tone: 'primary',
      });

  const discard = [nextCard, ...state.discard];

  if (duplicate) {
    players[playerIndex] = {
      ...player,
      status: 'eliminated',
      hasTurn: false,
    };

    const nextTurnPlayerId = getNextPlayerId(players, playerId);
    const nextPlayers = setActiveTurn(players, nextTurnPlayerId);

    const nextState: GameStateDTO = {
      ...state,
      players: nextPlayers,
      deck: nextDeck,
      discard,
      events: [eventBase, ...state.events],
      duplicateAlert: buildDuplicateAlert(player, nextCard.value),
      currentTurnPlayerId: nextTurnPlayerId,
      riskLevel: updateRiskLevel(nextPlayers),
    };

    const winner = findWinnerFromState(nextPlayers);

    if (winner) {
      return finalizeWinner(nextState, winner);
    }

    if (isRoundFinished(nextPlayers, nextDeck)) {
      return finalizeRound(nextState);
    }

    return nextState;
  }

  players[playerIndex] = {
    ...player,
    roundCards: [...player.roundCards, nextCard],
    hasTurn: false,
  };

  const hasSevenCards = players[playerIndex].roundCards.length >= 7;
  const nextTurnPlayerId = hasSevenCards ? null : getNextPlayerId(players, playerId);
  const nextPlayers = setActiveTurn(players, nextTurnPlayerId);

  const nextState: GameStateDTO = {
    ...state,
    players: nextPlayers,
    deck: nextDeck,
    discard,
    events: [eventBase, ...state.events],
    currentTurnPlayerId: nextTurnPlayerId,
    duplicateAlert: null,
    riskLevel: updateRiskLevel(nextPlayers),
  };

  const winner = findWinnerFromState(nextPlayers);

  if (winner) {
    return finalizeWinner(nextState, winner);
  }

  if (hasSevenCards || isRoundFinished(nextPlayers, nextDeck)) {
    return finalizeRound(nextState);
  }

  return nextState;
}

export function applyStand(state: GameStateDTO, playerId: string) {
  if (state.gamePhase !== 'playing' || state.currentTurnPlayerId !== playerId) {
    return state;
  }

  const players = clonePlayers(state.players);
  const playerIndex = getPlayerIndex(players, playerId);
  const player = players[playerIndex];

  if (!player) {
    return state;
  }

  players[playerIndex] = {
    ...player,
    status: 'stood',
    hasTurn: false,
  };

  const standEvent = createEvent({
    title: 'Jugador plantado',
    description: `${player.name} aseguró ${getRoundScore(player).roundScore} puntos.`,
    playerId: player.id,
    playerName: player.name,
    tone: 'warning',
  });

  const nextTurnPlayerId = getNextPlayerId(players, playerId);
  const nextPlayers = setActiveTurn(players, nextTurnPlayerId);
  const nextState: GameStateDTO = {
    ...state,
    players: nextPlayers,
    events: [standEvent, ...state.events],
    currentTurnPlayerId: nextTurnPlayerId,
    duplicateAlert: null,
    riskLevel: updateRiskLevel(nextPlayers),
  };

  const winner = findWinnerFromState(nextPlayers);

  if (winner) {
    return finalizeWinner(nextState, winner);
  }

  if (isRoundFinished(nextPlayers, state.deck)) {
    return finalizeRound(nextState);
  }

  return nextState;
}

export function advanceToNextRound(state: GameStateDTO) {
  if (state.gamePhase === 'playing') {
    return state;
  }

  const nextRound = state.currentRound + 1;
  const orderedPlayers = [...state.players];
  const starterIndex = state.roundStarterPlayerId ? orderedPlayers.findIndex((player) => player.id === state.roundStarterPlayerId) : -1;
  const nextStarter = orderedPlayers.find((player, index) => index > starterIndex && player.status !== 'eliminated') ?? orderedPlayers.find((player) => player.status !== 'eliminated') ?? orderedPlayers[0] ?? null;
  const resetPlayers = orderedPlayers.map((player) => ({
    ...player,
    roundCards: [],
    status: 'playing' as PlayerStatus,
    hasTurn: player.id === nextStarter?.id,
  }));

  const nextState: GameStateDTO = {
    ...state,
    currentRound: nextRound,
    currentTurnPlayerId: nextStarter?.id ?? null,
    roundStarterPlayerId: nextStarter?.id ?? null,
    players: setActiveTurn(resetPlayers, nextStarter?.id ?? null),
    deck: buildDeck(nextRound),
    discard: [],
    events: [
      createEvent({
        title: 'Nueva ronda',
        description: `La ronda ${nextRound} ya está activa.`,
        tone: 'primary',
      }),
      ...state.events,
    ],
    gamePhase: 'playing',
    roundSummary: null,
    duplicateAlert: null,
    winner: null,
    riskLevel: 0,
  };

  return nextState;
}

export function restartGame(state?: GameStateDTO) {
  const fresh = createInitialGameState();

  return state
    ? {
        ...fresh,
        gameId: state.gameId,
      }
    : fresh;
}
