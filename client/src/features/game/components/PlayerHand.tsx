import { AnimatePresence, motion } from 'framer-motion';

import { CardComponent } from './CardComponent';
import type { PlayerDTO } from '../types/game';

interface PlayerHandProps {
  player: PlayerDTO | undefined;
  latestCardId: string | null;
  isDuplicateFlash: boolean;
}

export function PlayerHand({ player, latestCardId, isDuplicateFlash }: PlayerHandProps) {
  const cards = player?.roundCards ?? [];

  return (
    <section className="player-hand">
      <div className="section-caption">
        <span className="eyebrow">Current Hand</span>
        <span className="risk-label">{player?.name ?? 'Waiting'}</span>
      </div>

      <div className={`cards-row hand-row ${isDuplicateFlash ? 'duplicate-flash' : ''}`}>
        <AnimatePresence initial={false} mode="popLayout">
          {cards.length > 0 ? (
              cards.map((card, index) => (
              <CardComponent
                key={card.id}
                  card={card}
                compact
                isLatest={card.id === latestCardId || index === cards.length - 1}
              />
            ))
          ) : (
            <motion.div
              key="empty-hand"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="hand-placeholder"
            >
              No cards yet.
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </section>
  );
}
