import type { PlayerStatus, RoundSummaryDTO } from '../types/game';

export function getPlayerStatusLabel(status: PlayerStatus) {
  switch (status) {
    case 'playing':
      return 'jugando';
    case 'stood':
      return 'plantado';
    case 'eliminated':
      return 'eliminado';
    default:
      return status;
  }
}

export function getRoundEndingLabel(reason: RoundSummaryDTO['endingReason']) {
  switch (reason) {
    case 'seven-cards':
      return 'siete cartas';
    case 'all-stand':
      return 'todos plantados';
    case 'all-eliminated':
      return 'todos eliminados';
    case 'deck-empty':
      return 'mazo agotado';
    default:
      return reason;
  }
}

export function getRiskLabel(value: number) {
  if (value >= 80) {
    return 'Crítico';
  }

  if (value >= 60) {
    return 'Riesgo moderado';
  }

  return 'Estable';
}
