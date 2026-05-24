import { AnimatePresence, motion } from 'framer-motion';

import { getPlayerStatusLabel, getRoundEndingLabel } from '../utils/labels';
import type { GameStateDTO } from '../types/game';

interface RoundSummaryModalProps {
  game: GameStateDTO | null;
  open: boolean;
  onNextRound: () => void;
}

export function RoundSummaryModal({ game, open, onNextRound }: RoundSummaryModalProps) {
  const summary = game?.roundSummary;

  return (
    <AnimatePresence>
      {open && summary ? (
        <motion.div className="modal-overlay" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} data-testid="round-summary-modal">
          <motion.div
            className="modal-card summary-modal"
            initial={{ scale: 0.92, y: 24, opacity: 0 }}
            animate={{ scale: 1, y: 0, opacity: 1 }}
            exit={{ scale: 0.94, y: 12, opacity: 0 }}
          >
            <div className="modal-header">
              <div>
                <p className="eyebrow">Resumen de ronda</p>
                <h3>Ronda {summary.round}</h3>
              </div>

              <span className="summary-pill">{getRoundEndingLabel(summary.endingReason)}</span>
            </div>

            <div className="summary-grid">
              {summary.players.map((player) => (
                <article key={player.playerId} className="summary-row">
                  <div>
                    <p className="summary-name">{player.playerName}</p>
                    <p className="summary-meta">
                      {getPlayerStatusLabel(player.status)} • Cartas {player.cards.length}
                    </p>
                  </div>

                  <div className="summary-score-block">
                    <span className="summary-score">{player.roundScore}</span>
                    {player.bonus ? <span className="summary-bonus">+{player.bonus}</span> : null}
                  </div>
                </article>
              ))}
            </div>

            <button type="button" className="primary-action" onClick={onNextRound} data-testid="next-round-button">
              <span className="material-symbols-outlined">arrow_forward</span>
              <span>Siguiente ronda</span>
            </button>
          </motion.div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
