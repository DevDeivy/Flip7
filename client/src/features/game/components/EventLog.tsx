import { motion } from 'framer-motion';

import type { GameEventDTO } from '../types/game';

interface EventLogProps {
  events: GameEventDTO[];
}

export function EventLog({ events }: EventLogProps) {
  return (
    <section className="panel log-panel" data-testid="event-log">
      <div className="panel-heading">
        <span className="material-symbols-outlined">history</span>
        <h3>Registro de jugadas</h3>
      </div>

      <div className="log-list custom-scrollbar">
        {events.slice(0, 9).map((event) => (
          <motion.article
            key={event.id}
            layout
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className={`log-entry tone-${event.tone}`}
            data-testid={`event-${event.title.toLowerCase().replace(/\s+/g, '-')}`}
          >
            <span className={`log-player tone-${event.tone}`}>{event.playerName ?? 'Sistema'}</span>
            <p className="log-message">
              {event.description}
              {typeof event.value === 'number' ? <span className="log-value"> {event.value}</span> : null}
            </p>
          </motion.article>
        ))}
      </div>
    </section>
  );
}
