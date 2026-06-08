import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';

import { ActionPanel } from '../components/ActionPanel';
import { DuplicateCardAlert } from '../components/DuplicateCardAlert';
import { EventLog } from '../components/EventLog';
import { GameTable } from '../components/GameTable';
import { PlayerSidebar } from '../components/PlayerSidebar';
import { WinnerModal } from '../components/WinnerModal';
import { useGameStore } from '../store/gameStore';

export function VsAIPage() {
  const navigate = useNavigate();
  const game = useGameStore((state) => state.game);
  const isBusy = useGameStore((state) => state.isBusy);
  const error = useGameStore((state) => state.error);
  const initializeAiGame = useGameStore((state) => state.initializeAiGame);
  const restartGame = useGameStore((state) => state.restartGame);
  const drawCard = useGameStore((state) => state.drawCard);
  const stand = useGameStore((state) => state.stand);
  const dismissDuplicateAlert = useGameStore((state) => state.dismissDuplicateAlert);
  const playerAlias = useGameStore((state) => state.playerAlias);

  const [copiedCode, setCopiedCode] = useState(false);

  const viewerPlayer = useMemo(() => {
    if (!game) return undefined;
    const normalizedAlias = playerAlias.trim().toLowerCase();
    if (!normalizedAlias) return undefined;
    return game.players.find((p) => p.name.trim().toLowerCase() === normalizedAlias);
  }, [game, playerAlias]);

  const aiPlayer = useMemo(() => game?.players.find((player) => player.aiControlled), [game]);
  const currentTurnPlayer = useMemo(() => {
    if (!game || !game.currentTurnPlayerId) return undefined;
    return game.players.find((player) => player.id === game.currentTurnPlayerId);
  }, [game]);

  const viewerCards = viewerPlayer ? [...viewerPlayer.roundCards, ...viewerPlayer.specialCards] : [];
  const latestCardId = viewerCards.length > 0 ? viewerCards[viewerCards.length - 1].id : null;
  const canAct = Boolean(
    game
    && game.gamePhase === 'playing'
    && viewerPlayer
    && game.currentTurnPlayerId === viewerPlayer.id,
  );

  const currentTurnPlayerName = currentTurnPlayer?.name ?? null;
  const isAiTurn = Boolean(currentTurnPlayer?.aiControlled);

  useEffect(() => {
    void initializeAiGame();
  }, [initializeAiGame]);

  if (!game) {
    return (
      <div className="arena-shell vsai-shell">
        <div className="arena-vignette" aria-hidden="true" />
        <div className="arena-radar" aria-hidden="true" />

        <header className="topbar topbar-futuristic">
          <h1 className="brand">FLIP7</h1>
          <div className="topbar-meta-controls">
            <div className="table-meta">
              <span className="eyebrow">Modo</span>
              <span className="meta-value">VS AI</span>
            </div>
            <button type="button" className="secondary-action back-nav-action" onClick={handleExitToHome}>
              <span className="material-symbols-outlined">logout</span>
              <span>Salir</span>
            </button>
          </div>
        </header>

        <main className="multiplayer-select-main">
          <section className="multiplayer-select-hero">
            <h2>INICIANDO PARTIDA</h2>
            <p>{isBusy ? 'Preparando la mesa contra FLIP7 AI...' : 'Cargando estado del juego...'}</p>
          </section>

          {error ? <div className="error-banner">{error}</div> : null}
        </main>
      </div>
    );
  }

  function handleExitToHome() {
    void restartGame();
    navigate('/');
  }

  async function handleCopy(code: string) {
    try {
      await navigator.clipboard.writeText(code);
      setCopiedCode(true);
      window.setTimeout(() => setCopiedCode(false), 1400);
    } catch {
      setCopiedCode(false);
    }
  }

  return (
    <div className="arena-shell vsai-shell">
      <div className="arena-vignette" aria-hidden="true" />
      <div className="arena-radar" aria-hidden="true" />

      <header className="topbar topbar-futuristic">
        <h1 className="brand">FLIP7</h1>
        <div className="topbar-meta-controls">
          <div className="table-meta">
            <span className="eyebrow">Modo</span>
            <span className="meta-value">VS AI</span>
          </div>
          <button type="button" className="secondary-action back-nav-action" onClick={handleExitToHome}>
            <span className="material-symbols-outlined">logout</span>
            <span>Salir</span>
          </button>
        </div>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}

      <main className="vsai-layout">
        <section className="game-grid vsai-battlefield">
          <aside className="sidebar sidebar-left">
            <PlayerSidebar players={game?.players ?? []} currentPlayerId={game?.currentTurnPlayerId ?? null} />
            <EventLog
              events={game?.events ?? []}
              viewerName={viewerPlayer?.name ?? playerAlias}
              aiPlayerName={aiPlayer?.name ?? null}
            />
          </aside>

          <GameTable
            game={game!}
            activePlayer={viewerPlayer}
            opponentPlayer={aiPlayer}
            latestCardId={latestCardId}
            duplicateFlash={false}
          />

          <aside className="sidebar sidebar-right">
            <section className="panel ai-config-panel">
              <h2 className="panel-title">Interfaz AI — Partido local</h2>

              {canAct ? (
                <ActionPanel
                  disabled={isBusy}
                  canAct={canAct}
                  onDraw={() => { if (canAct && viewerPlayer) void drawCard(viewerPlayer.id); }}
                  onStand={() => { if (canAct && viewerPlayer) void stand(viewerPlayer.id); }}
                />
              ) : (
                <div className="ai-status-container">
                <div className={`waiting-turn-copy ${isAiTurn ? 'ai-thinking' : ''}`}>
                  <span className="material-symbols-outlined">
                    {isAiTurn ? 'memory' : 'hourglass_top'}
                  </span>
                  <p>
                    {isAiTurn ? 'FLIP7 AI está procesando datos...' : 'Espera a que sea tu turno.'}
                    {currentTurnPlayerName && !isAiTurn ? ` Ahora juega ${currentTurnPlayerName}.` : ''}
                  </p>
                </div>
              </div>
            )}

            <div className="ai-footer-row">
              <button className="secondary-action">Model: flip7-ai:latest</button>
            </div>
          </section>

          {game?.aiReason && (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="ai-thought-box persistent-thought"
            >
              <div className="thought-header">
                <span className="material-symbols-outlined">psychology</span>
                <span>Pensamiento de la IA</span>
              </div>
              <p className="thought-content">{game.aiReason}</p>
            </motion.div>
          )}
        </aside>
        </section>
      </main>

      <footer className="footer footer-futuristic">
        <div className="footer-left">
          <div className="metric-line">
            <span className="eyebrow">Ronda</span>
            <span className="metric-inline is-primary">{String(game?.currentRound ?? 0).padStart(2, '0')}</span>
          </div>
          <span className="divider" aria-hidden="true" />
          <div className="metric-line">
            <span className="eyebrow">Cartas restantes</span>
            <span className="metric-inline">{game?.deck.length ?? 0}</span>
          </div>
        </div>

        <div className="footer-right">
          <div className="latency">
            <span className="material-symbols-outlined">memory</span>
            <span>AI core: flip7-ai:latest</span>
          </div>
          <p className="copyright">© 2026 FLIP7 TCG</p>
        </div>
      </footer>

      <WinnerModal game={game!} open={game?.gamePhase === 'winner'} onRestart={() => void restartGame()} />
      <DuplicateCardAlert alert={game?.duplicateAlert ?? null} onClose={() => dismissDuplicateAlert()} />
    </div>
  );
}

export default VsAIPage;
