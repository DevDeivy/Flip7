import { PlayerHand } from './PlayerHand';
import { RiskMeter } from './RiskMeter';
import { getRiskLabel } from '../utils/labels';
import type { GameStateDTO, PlayerDTO } from '../types/game';

interface GameTableProps {
  game: GameStateDTO;
  activePlayer: PlayerDTO | undefined;
  opponentPlayer?: PlayerDTO | undefined;
  latestCardId: string | null;
  duplicateFlash: boolean;
}

export function GameTable({ game, activePlayer, opponentPlayer, latestCardId, duplicateFlash }: GameTableProps) {
  const uniqueCards = activePlayer?.roundCards.length ?? 0;
  const roundScore = activePlayer?.roundCards.reduce((total, card) => total + card.value, 0) ?? 0;
  const opponentCards = opponentPlayer ? [...opponentPlayer.roundCards, ...opponentPlayer.specialCards] : [];
  const opponentLatestCardId = opponentCards.length > 0
    ? opponentCards[opponentCards.length - 1].id
    : null;
  const activeCards = activePlayer ? [...activePlayer.roundCards, ...activePlayer.specialCards] : [];
  const activeLatestCardId = activeCards.length > 0 ? activeCards[activeCards.length - 1].id : latestCardId;

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

      <section className="tabletop duel-tabletop" aria-label="Cartas visibles en la mesa">
        <div className="risk-halo" aria-hidden="true" />

        <div className="duel-table-rails">
          {opponentPlayer ? (
            <PlayerHand
              player={opponentPlayer}
              latestCardId={opponentLatestCardId}
              isDuplicateFlash={false}
              title="AI Model"
              subtitle="Cartas de la IA"
              className="opponent-hand"
            />
          ) : null}

          <div className="cards-row tabletop-row">
            {activeCards.length ? (
              <PlayerHand player={activePlayer} latestCardId={activeLatestCardId} isDuplicateFlash={duplicateFlash} title="Tu mano" subtitle="Las cartas que ves frente a ti" className="player-hand-main" />
            ) : (
              <div className="table-empty-state">
                <span className="eyebrow">Tu mano está lista</span>
                <span className="table-empty-copy">Cuando te toque, roba para comenzar la ronda.</span>
              </div>
            )}
          </div>
        </div>
      </section>

      <RiskMeter value={game.riskLevel} label={`Estabilidad del sistema · ${getRiskLabel(game.riskLevel)}`} />
    </section>
  );
}
