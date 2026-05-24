import { PlayerHand } from './PlayerHand';
import { RiskMeter } from './RiskMeter';
import { getRiskLabel } from '../utils/labels';
import type { GameStateDTO, PlayerDTO } from '../types/game';

interface GameTableProps {
  game: GameStateDTO;
  activePlayer: PlayerDTO | undefined;
  latestCardId: string | null;
  duplicateFlash: boolean;
}

export function GameTable({ game, activePlayer, latestCardId, duplicateFlash }: GameTableProps) {
  const uniqueCards = activePlayer?.roundCards.length ?? 0;
  const roundScore = activePlayer?.roundCards.reduce((total, card) => total + card.value, 0) ?? 0;

  return (
    <section className="center-stage">
      <section className="scoreboard" aria-label="Estado actual de la partida">
        <div className="metric-block">
          <p className="eyebrow">Puntaje de ronda</p>
          <p className="metric-value is-primary">{roundScore}</p>
        </div>

        <div className="metric-block">
          <p className="eyebrow">Cartas únicas</p>
          <p className="metric-value">{uniqueCards} / 7</p>
        </div>
      </section>

      <section className="tabletop" aria-label="Cartas del jugador en la mesa">
        <div className="risk-halo" aria-hidden="true" />

        <div className="cards-row tabletop-row">
          {activePlayer?.roundCards.length ? (
            <PlayerHand player={activePlayer} latestCardId={latestCardId} isDuplicateFlash={duplicateFlash} />
          ) : (
            <div className="table-empty-state">
              <span className="eyebrow">Mesa lista</span>
              <span className="table-empty-copy">Roba para comenzar la ronda.</span>
            </div>
          )}
        </div>
      </section>

      <RiskMeter value={game.riskLevel} label={`Estabilidad del sistema · ${getRiskLabel(game.riskLevel)}`} />
    </section>
  );
}
