import { motion } from 'framer-motion';

import type { GameEventDTO } from '../types/game';

interface EventLogProps {
  events: GameEventDTO[];
  viewerName?: string | null;
  aiPlayerName?: string | null;
}

function formatActor(name: string | undefined, viewerName?: string | null, aiPlayerName?: string | null) {
  if (!name) {
    return 'Sistema';
  }

  const normalizedActor = name.trim().toLowerCase();
  if (viewerName && normalizedActor === viewerName.trim().toLowerCase()) {
    return 'Tú';
  }

  if (aiPlayerName && normalizedActor === aiPlayerName.trim().toLowerCase()) {
    return aiPlayerName;
  }

  return name;
}

function formatEntry(event: GameEventDTO, viewerName?: string | null, aiPlayerName?: string | null) {
  const actor = formatActor(event.playerName, viewerName, aiPlayerName);
  const drawMatch = event.description.match(/^(.*) sac[óo] un (\d+)$/i);
  const decisionMatch = event.description.match(/^(.*) decide (hit|stand|play) porque (.*)$/i);
  const standMatch = event.description.match(/^(.*) se ha plantado con (\d+) puntos\.?$/i);
  const repeatMatch = event.description.match(/^Carta repetida\. (.*) ha sido eliminado\.?$/i);
  const freezeMatch = event.description.match(/^FREEZE! (.*) se congela con (\d+) puntos\.?$/i);
  const flipMatch = event.description.match(/^FLIP 7! (.*) completó 7 cartas\.?$/i);
  const secondChanceMatch = event.description.match(/^Segunda Oportunidad! (.*) se salvó del duplicado de (\d+)$/i);

  if (decisionMatch) {
    const decision = decisionMatch[2].toLowerCase() === 'hit' || decisionMatch[2].toLowerCase() === 'play' ? 'robar' : 'plantarse';
    return {
      actor,
      headline: `${actor} decidió ${decision}`,
      detail: decisionMatch[3],
      value: undefined,
    };
  }

  if (drawMatch) {
    return {
      actor,
      headline: actor === 'Tú' ? 'Sacaste carta' : `${actor} sacó carta`,
      detail: `${actor === 'Tú' ? 'Sacaste' : 'Sacó'} ${drawMatch[2]}`,
      value: Number(drawMatch[2]),
    };
  }

  if (standMatch) {
    return {
      actor,
      headline: `${actor} se plantó`,
      detail: `Quedó con ${standMatch[2]} puntos`,
      value: Number(standMatch[2]),
    };
  }

  if (repeatMatch) {
    return {
      actor,
      headline: `${repeatMatch[1]} fue eliminado`,
      detail: 'Carta repetida',
      value: undefined,
    };
  }

  if (freezeMatch) {
    return {
      actor,
      headline: `${freezeMatch[1]} se congeló`,
      detail: `Con ${freezeMatch[2]} puntos`,
      value: Number(freezeMatch[2]),
    };
  }

  if (flipMatch) {
    return {
      actor,
      headline: `${flipMatch[1]} completó FLIP 7`,
      detail: 'La ronda terminó por 7 cartas únicas',
      value: undefined,
    };
  }

  if (secondChanceMatch) {
    return {
      actor,
      headline: `${secondChanceMatch[1]} se salvó`,
      detail: `Segunda oportunidad contra duplicado de ${secondChanceMatch[2]}`,
      value: Number(secondChanceMatch[2]),
    };
  }

  return {
    actor,
    headline: event.description,
    detail: '',
    value: event.value,
  };
}

export function EventLog({ events, viewerName, aiPlayerName }: EventLogProps) {
  return (
    <section className="panel log-panel" data-testid="event-log">
      <div className="panel-heading">
        <span className="material-symbols-outlined">history</span>
        <h3>Registro de jugadas</h3>
      </div>

      <div className="log-list custom-scrollbar">
        {events.slice(0, 20).map((event) => (
          <motion.article
            key={event.id}
            layout
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className={`log-entry tone-${event.tone}`}
            data-testid={`event-${event.title.toLowerCase().replace(/\s+/g, '-')}`}
          >
            {(() => {
              const formatted = formatEntry(event, viewerName, aiPlayerName);

              return (
                <>
                  <div className="log-headline-row">
                    <span className={`log-player tone-${event.tone}`}>{formatted.actor}</span>
                    <span className="log-headline">{formatted.headline}</span>
                  </div>
                  {formatted.detail ? <p className="log-message">{formatted.detail}</p> : null}
                  {typeof formatted.value === 'number' ? <span className="log-value">{formatted.value}</span> : null}
                </>
              );
            })()}
          </motion.article>
        ))}
        {events.length === 0 ? (
          <p className="log-empty">Todavía no hay jugadas registradas.</p>
        ) : null}
      </div>
    </section>
  );
}
