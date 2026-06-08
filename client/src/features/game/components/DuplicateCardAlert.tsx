import { AnimatePresence, motion } from 'framer-motion';

import type { DuplicateAlertDTO } from '../types/game';

interface DuplicateCardAlertProps {
  alert: DuplicateAlertDTO | null;
  onClose: () => void;
}

export function DuplicateCardAlert({ alert, onClose }: DuplicateCardAlertProps) {
  return (
    <AnimatePresence>
      {alert ? (
        <motion.div className="modal-overlay duplicate-overlay" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <motion.div
            className="modal-card duplicate-modal"
            initial={{ scale: 0.95, y: 16, opacity: 0 }}
            animate={{ scale: 1, y: 0, opacity: 1 }}
            exit={{ scale: 0.96, y: 12, opacity: 0 }}
            data-testid="duplicate-alert"
          >
            <p className="eyebrow">Alerta de duplicado</p>
            <h3>{alert.playerName}</h3>
            
            <div className="duplicate-visual">
              <div className="duplicate-card-mini">
                <span className="mini-value">{alert.cardValue}</span>
                <span className="mini-label">REPETIDA</span>
              </div>
            </div>

            <p className="duplicate-copy">{alert.message}</p>
            <button type="button" className="secondary-action" onClick={onClose}>
              Reconocer
            </button>
          </motion.div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
