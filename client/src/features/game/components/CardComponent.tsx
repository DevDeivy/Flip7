import { motion } from 'framer-motion';

import type { GameCardDTO } from '../types/game';

const SPECIAL_CARD_META: Record<number, { label: string; subtitle: string; variant: string }> = {
  100: { label: 'FREEZE', subtitle: 'Se congela', variant: 'freeze' },
  101: { label: 'FLIP 3', subtitle: 'Roba 3', variant: 'flip-three' },
  102: { label: 'SECOND', subtitle: 'Segunda oportunidad', variant: 'second-chance' },
  200: { label: 'x2', subtitle: 'Multiplica', variant: 'multiplier' },
  201: { label: '+2', subtitle: 'Bonus', variant: 'bonus' },
  202: { label: '+4', subtitle: 'Bonus', variant: 'bonus' },
  203: { label: '+6', subtitle: 'Bonus', variant: 'bonus' },
  204: { label: '+8', subtitle: 'Bonus', variant: 'bonus' },
  205: { label: '+10', subtitle: 'Bonus', variant: 'bonus' },
};

interface CardComponentProps {
  card: GameCardDTO;
  isLatest?: boolean;
  isDuplicate?: boolean;
  compact?: boolean;
}

export function CardComponent({ card, isLatest = false, isDuplicate = false, compact = false }: CardComponentProps) {
  const specialCard = SPECIAL_CARD_META[card.value];
  const className = [
    'game-card',
    compact ? 'compact-card' : '',
    isLatest ? 'primary' : 'neutral',
    isDuplicate ? 'duplicate-card' : '',
    specialCard ? 'special-card' : '',
    specialCard ? `special-${specialCard.variant}` : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <motion.article
      layout
      initial={{ opacity: 0, y: 18, rotate: -6, scale: 0.92 }}
      animate={{ opacity: 1, y: 0, rotate: 0, scale: 1 }}
      exit={{ opacity: 0, y: -24, scale: 0.88 }}
      transition={{ type: 'spring', stiffness: 300, damping: 24 }}
      className={className}
    >
      <div className="game-card-sheen" aria-hidden="true" />
      {specialCard ? <span className="game-card-tag">SPECIAL</span> : null}
      <div className="game-card-main-content">
        <span className="game-card-value">{specialCard ? specialCard.label : card.value}</span>
      </div>
      {isLatest ? <span className={`game-card-badge ${specialCard ? 'special-badge' : ''}`}>New</span> : null}
      {isDuplicate ? <span className="game-card-badge duplicate">Dup</span> : null}
    </motion.article>
  );
}
