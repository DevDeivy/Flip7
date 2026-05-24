export type PlayerStatus = 'playing' | 'stood' | 'eliminated';
export type GamePhase = 'idle' | 'playing' | 'roundSummary' | 'winner';
export type EventTone = 'primary' | 'warning' | 'danger' | 'muted';

export interface GameCardDTO {
  id: string;
  value: number;
}

export interface PlayerDTO {
  id: string;
  name: string;
  totalScore: number;
  roundCards: GameCardDTO[];
  status: PlayerStatus;
  hasTurn: boolean;
}

export interface GameEventDTO {
  id: string;
  title: string;
  description: string;
  playerId?: string;
  playerName?: string;
  value?: number;
  tone: EventTone;
  timestamp: string;
}

export interface DuplicateAlertDTO {
  playerId: string;
  playerName: string;
  cardValue: number;
  message: string;
}

export interface RoundSummaryPlayerDTO {
  playerId: string;
  playerName: string;
  roundScore: number;
  totalScore: number;
  status: PlayerStatus;
  cards: number[];
  bonus: number;
}

export interface RoundSummaryDTO {
  round: number;
  endingReason: 'seven-cards' | 'all-stand' | 'all-eliminated' | 'deck-empty';
  players: RoundSummaryPlayerDTO[];
}

export interface WinnerDTO {
  playerId: string;
  playerName: string;
  totalScore: number;
}

export interface GameStateDTO {
  gameId: string;
  gamePhase: GamePhase;
  currentRound: number;
  currentTurnPlayerId: string | null;
  roundStarterPlayerId: string | null;
  players: PlayerDTO[];
  deck: GameCardDTO[];
  discard: GameCardDTO[];
  events: GameEventDTO[];
  riskLevel: number;
  roundSummary: RoundSummaryDTO | null;
  duplicateAlert: DuplicateAlertDTO | null;
  winner: WinnerDTO | null;
}

export interface GameResponseDTO {
  game: GameStateDTO;
}

export interface GameActionResultDTO extends GameResponseDTO {
  event?: GameEventDTO;
}

export interface GameTestPlayerConfig {
  id?: string;
  name: string;
  totalScore?: number;
  roundCards?: number[];
  status?: PlayerStatus;
  hasTurn?: boolean;
}

export interface GameTestStateConfig {
  currentRound?: number;
  currentTurnPlayerId?: string | null;
  roundStarterPlayerId?: string | null;
  players?: GameTestPlayerConfig[];
  deck?: number[];
  discard?: number[];
  events?: Array<Pick<GameEventDTO, 'title' | 'description' | 'tone'>>;
  gamePhase?: GamePhase;
  roundSummary?: RoundSummaryDTO | null;
  duplicateAlert?: DuplicateAlertDTO | null;
  winner?: WinnerDTO | null;
  riskLevel?: number;
}

