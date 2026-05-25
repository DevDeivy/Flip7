import { motion } from 'framer-motion';

import type { GameCardDTO } from '../types/game';

interface CardComponentProps {
  card: GameCardDTO;
  isLatest?: boolean;
  isDuplicate?: boolean;
  compact?: boolean;
}

export function CardComponent({ card, isLatest = false, isDuplicate = false, compact = false }: CardComponentProps) {
  return (
    <motion.article
      layout
      initial={{ opacity: 0, y: 18, rotate: -6, scale: 0.92 }}
      animate={{ opacity: 1, y: 0, rotate: 0, scale: 1 }}
      exit={{ opacity: 0, y: -24, scale: 0.88 }}
      transition={{ type: 'spring', stiffness: 300, damping: 24 }}
      className={`game-card ${compact ? 'compact-card' : ''} ${isLatest ? 'primary' : 'neutral'} ${isDuplicate ? 'duplicate-card' : ''}`}
    >
      <div className="game-card-sheen" aria-hidden="true" />
      <span className="game-card-value">{card.value}</span>
      {isLatest ? <span className="game-card-badge">New</span> : null}
      {isDuplicate ? <span className="game-card-badge duplicate">Dup</span> : null}
    </motion.article>
  );
}
