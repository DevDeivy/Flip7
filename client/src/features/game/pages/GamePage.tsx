import { useEffect, useMemo } from 'react';

import { ActionPanel } from '../components/ActionPanel';
import { DuplicateCardAlert } from '../components/DuplicateCardAlert';
import { EventLog } from '../components/EventLog';
import { GameTable } from '../components/GameTable';
import { PlayerSidebar } from '../components/PlayerSidebar';
import { RoundSummaryModal } from '../components/RoundSummaryModal';
import { WinnerModal } from '../components/WinnerModal';
import { useGameActions } from '../hooks/useGameActions';
import { useGameState } from '../hooks/useGameState';
import { useGameStore } from '../store/gameStore';

export function GamePage() {
  const game = useGameState();
  const { initializeGame, drawCard, stand, nextRound, restartGame, dismissDuplicateAlert } = useGameActions();
  const isBusy = useGameStore((state) => state.isBusy);
  const error = useGameStore((state) => state.error);

  useEffect(() => {
    void initializeGame();
  }, [initializeGame]);

  const activePlayer = useMemo(
    () => game?.players.find((player) => player.id === game.currentTurnPlayerId) ?? game?.players.find((player) => player.status === 'playing'),
    [game],
  );

  const latestCardId = activePlayer && activePlayer.roundCards.length > 0 ? activePlayer.roundCards[activePlayer.roundCards.length - 1].id : null;
  const canAct = Boolean(game && game.gamePhase === 'playing' && activePlayer && activePlayer.status === 'playing');

  if (!game) {
    return (
      <div className="arena-shell loading-shell">
        <div className="loading-card panel">
          <p className="eyebrow">Iniciando arena</p>
          <h1 className="brand">FLIP7</h1>
          <p className="loading-copy">Sincronizando la simulación local...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="arena-shell">
      <div className="arena-vignette" aria-hidden="true" />
      <div className="arena-radar" aria-hidden="true" />

      <header className="topbar">
        <h1 className="brand">FLIP7</h1>
        <div className="table-meta">
          <span className="eyebrow">ID de mesa</span>
          <span className="meta-value">#DELTA-9</span>
        </div>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}

      <main className="game-grid">
        <PlayerSidebar players={game.players} currentPlayerId={game.currentTurnPlayerId} />

        <GameTable game={game} activePlayer={activePlayer} latestCardId={latestCardId} duplicateFlash={Boolean(game.duplicateAlert)} />

        <aside className="sidebar sidebar-right">
          <ActionPanel
            disabled={isBusy}
            canAct={canAct}
            onDraw={() => {
              if (game.currentTurnPlayerId) {
                void drawCard(game.currentTurnPlayerId);
              }
            }}
            onStand={() => {
              if (game.currentTurnPlayerId) {
                void stand(game.currentTurnPlayerId);
              }
            }}
          />

          <EventLog events={game.events} />
        </aside>
      </main>

      <footer className="footer">
        <div className="footer-left">
          <div className="metric-line">
            <span className="eyebrow">Ronda</span>
            <span className="metric-inline is-primary">{String(game.currentRound).padStart(2, '0')}</span>
          </div>
          <span className="divider" aria-hidden="true" />
          <div className="metric-line">
            <span className="eyebrow">Cartas restantes</span>
            <span className="metric-inline">{game.deck.length}</span>
          </div>
        </div>

        <div className="footer-right">
          <div className="latency">
            <span className="material-symbols-outlined">sensors</span>
            <span>Latencia: 24 ms</span>
          </div>
          <p className="copyright">© 2026 FLIP7 TCG</p>
        </div>
      </footer>

      <DuplicateCardAlert alert={game.duplicateAlert} onClose={dismissDuplicateAlert} />
      <RoundSummaryModal game={game} open={game.gamePhase === 'roundSummary'} onNextRound={() => void nextRound()} />
      <WinnerModal game={game} open={game.gamePhase === 'winner'} onRestart={() => void restartGame()} />
    </div>
  );
}
