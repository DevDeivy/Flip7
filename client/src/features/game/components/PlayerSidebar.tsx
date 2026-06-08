import { motion } from 'framer-motion';

import { getPlayerStatusLabel } from '../utils/labels';
import type { PlayerDTO } from '../types/game';

interface PlayerSidebarProps {
  players: PlayerDTO[];
  currentPlayerId: string | null;
}

export function PlayerSidebar({ players, currentPlayerId }: PlayerSidebarProps) {
  return (
    <aside className="sidebar sidebar-left">
      <section className="panel">
        <div className="section-header">
          <span className="material-symbols-outlined">groups</span>
          <h2>Combatientes</h2>
        </div>

        <div className="stack">
          {players.map((player) => {
            const isActive = currentPlayerId === player.id;
            const toneClass = player.status === 'stood' ? 'warning' : player.status === 'eliminated' ? 'muted' : 'playing';

            return (
              <motion.article
                key={player.id}
                layout
                initial={{ opacity: 0, x: -12 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.22 }}
                className={`player-card ${isActive ? 'active' : ''} ${toneClass} ${player.status}`}
                data-testid={`player-card-${player.name.toLowerCase()}`}
                data-active-player={isActive ? 'true' : 'false'}
                data-player-id={player.id}
              >
                <div className="avatar" aria-hidden="true">
                  <span>{player.name.slice(0, 1)}</span>
                </div>

                <div className="player-content">
                  <div className="player-row">
                    <p className="player-name">{player.name}</p>
                    <p className="player-score">{player.totalScore}</p>
                  </div>

                  <p className="player-state">
                    {getPlayerStatusLabel(player.status)} • {player.roundCards.length} cartas
                  </p>
                </div>
              </motion.article>
            );
          })}
        </div>
      </section>
    </aside>
  );
}
