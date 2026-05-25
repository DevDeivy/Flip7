import { AnimatePresence, motion } from 'framer-motion';

import type { GameStateDTO } from '../types/game';

interface WinnerModalProps {
  game: GameStateDTO | null;
  open: boolean;
  onRestart: () => void;
}

export function WinnerModal({ game, open, onRestart }: WinnerModalProps) {
  const winner = game?.winner;

  return (
    <AnimatePresence>
      {open && winner ? (
        <motion.div className="modal-overlay winner-overlay" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <motion.div
            className="modal-card winner-modal"
            initial={{ scale: 0.9, y: 28, opacity: 0 }}
            animate={{ scale: 1, y: 0, opacity: 1 }}
            exit={{ scale: 0.92, y: 14, opacity: 0 }}
            data-testid="winner-modal"
          >
            <p className="eyebrow">Ganador detectado</p>
            <h3>{winner.playerName}</h3>
            <p className="winner-score">{winner.totalScore}</p>
            <p className="winner-copy">Alcanzó 200 puntos y ganó la partida.</p>

            <button type="button" className="primary-action" onClick={onRestart} data-testid="restart-button">
              <span className="material-symbols-outlined">refresh</span>
              <span>Jugar de nuevo</span>
            </button>
          </motion.div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
