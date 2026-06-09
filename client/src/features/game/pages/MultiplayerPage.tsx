import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { ActionPanel } from '../components/ActionPanel';
import { DuplicateCardAlert } from '../components/DuplicateCardAlert';
import { EventLog } from '../components/EventLog';
import { GameTable } from '../components/GameTable';
import { PlayerSidebar } from '../components/PlayerSidebar';
import { RoundSummaryModal } from '../components/RoundSummaryModal';
import { WinnerModal } from '../components/WinnerModal';
import { useGameStore } from '../store/gameStore';

export function MultiplayerPage() {
  const navigate = useNavigate();
  const game = useGameStore((state) => state.game);
  const room = useGameStore((state) => state.room);
  const isBusy = useGameStore((state) => state.isBusy);
  const error = useGameStore((state) => state.error);
  const createRoom = useGameStore((state) => state.createRoom);
  const joinRoom = useGameStore((state) => state.joinRoom);
  const refreshRoom = useGameStore((state) => state.refreshRoom);
  const startRoom = useGameStore((state) => state.startRoom);
  const drawCard = useGameStore((state) => state.drawCard);
  const stand = useGameStore((state) => state.stand);
  const nextRound = useGameStore((state) => state.nextRound);
  const dismissDuplicateAlert = useGameStore((state) => state.dismissDuplicateAlert);
  const bootstrapTestGame = useGameStore((state) => state.bootstrapTestGame);
  const restartGame = useGameStore((state) => state.restartGame);
  const playerAlias = useGameStore((state) => state.playerAlias);

  const [hostName, setHostName] = useState('');
  const [joinCode, setJoinCode] = useState('');
  const [joinName, setJoinName] = useState('');
  const [copiedRoomCode, setCopiedRoomCode] = useState(false);

  const isPlaywrightTestMode = typeof window !== 'undefined'
    && Boolean((window as Window & { __FLIP7_TEST__?: { state?: unknown } }).__FLIP7_TEST__?.state);

  useEffect(() => {
    if (!isPlaywrightTestMode || game || room) {
      return;
    }

    bootstrapTestGame();
  }, [bootstrapTestGame, game, isPlaywrightTestMode, room]);

  const viewerPlayer = useMemo(() => {
    if (!game) {
      return undefined;
    }

    if (isPlaywrightTestMode && game.currentTurnPlayerId) {
      return game.players.find((player) => player.id === game.currentTurnPlayerId);
    }

    const normalizedAlias = playerAlias.trim().toLowerCase();
    if (!normalizedAlias) {
      return undefined;
    }

    return game.players.find((player) => player.name.trim().toLowerCase() === normalizedAlias);
  }, [game, isPlaywrightTestMode, playerAlias]);

  const latestCardId = viewerPlayer && viewerPlayer.roundCards.length > 0 ? viewerPlayer.roundCards[viewerPlayer.roundCards.length - 1].id : null;
  const canAct = Boolean(
    game
    && game.gamePhase === 'playing'
    && viewerPlayer
    && viewerPlayer.status === 'playing'
    && game.currentTurnPlayerId === viewerPlayer.id,
  );

  const currentTurnPlayerName = useMemo(() => {
    if (!game || !game.currentTurnPlayerId) {
      return null;
    }

    return game.players.find((player) => player.id === game.currentTurnPlayerId)?.name ?? null;
  }, [game]);

  useEffect(() => {
    if (!room) {
      return;
    }

    const timer = window.setInterval(() => {
      void refreshRoom();
    }, 2000);

    return () => {
      window.clearInterval(timer);
    };
  }, [room, refreshRoom]);

  async function handleCreateRoom(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await createRoom(hostName);
  }

  async function handleJoinRoom(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await joinRoom(joinCode, joinName);
  }

  async function handleCopyRoomCode(code: string) {
    try {
      await navigator.clipboard.writeText(code);
      setCopiedRoomCode(true);
      window.setTimeout(() => setCopiedRoomCode(false), 1400);
    } catch {
      setCopiedRoomCode(false);
    }
  }

  function handleExitToHome() {
    void restartGame();
    navigate('/');
  }

  if (!game && !room) {
    return (
      <div className="arena-shell multiplayer-select-shell">
        <div className="arena-vignette" aria-hidden="true" />
        <div className="arena-radar" aria-hidden="true" />

        <header className="topbar">
          <h1 className="brand">FLIP7</h1>
          <button type="button" className="secondary-action back-nav-action" onClick={handleExitToHome}>
            <span className="material-symbols-outlined">arrow_back</span>
            <span>Volver</span>
          </button>
        </header>

        <main className="multiplayer-select-main">
          <section className="multiplayer-select-hero">
            <h2>SELECCION DE PARTIDA</h2>
            <p>Elige tu camino hacia la victoria</p>
          </section>

          <section className="multiplayer-select-grid" aria-label="Opciones de partida multijugador">
            <article className="multiplayer-select-card multiplayer-select-card-primary">
              <div className="multiplayer-select-overlay" aria-hidden="true" />
              <div className="multiplayer-select-icon multiplayer-select-icon-primary">
                <span className="material-symbols-outlined multiplayer-select-symbol-fill">add_circle</span>
              </div>

              <h3>CREAR SALA</h3>
              <p>Configura tu mesa y desafia a tus amigos en una sesion personalizada.</p>

              <form className="multiplayer-select-form" onSubmit={handleCreateRoom}>
                <label className="setup-label" htmlFor="host-name">Tu nombre</label>
                <input
                  id="host-name"
                  className="setup-input"
                  type="text"
                  value={hostName}
                  onChange={(event) => setHostName(event.target.value)}
                  placeholder="Ej: Erik"
                />

                <button type="submit" className="primary-action" disabled={isBusy}>
                  <span>{isBusy ? 'CREANDO...' : 'CREAR NUEVA SALA'}</span>
                  <span className="material-symbols-outlined">arrow_forward</span>
                </button>
              </form>
            </article>

            <article className="multiplayer-select-card multiplayer-select-card-secondary">
              <div className="multiplayer-select-overlay multiplayer-select-overlay-secondary" aria-hidden="true" />
              <div className="multiplayer-select-icon multiplayer-select-icon-secondary">
                <span className="material-symbols-outlined">group_add</span>
              </div>

              <h3>UNIRSE A PARTIDA</h3>
              <p>Introduce un codigo de invitacion para entrar en una mesa activa.</p>

              <form className="multiplayer-select-form" onSubmit={handleJoinRoom}>
                <label className="setup-label" htmlFor="room-code">Codigo de sala</label>
                <input
                  id="room-code"
                  className="setup-input"
                  type="text"
                  value={joinCode}
                  onChange={(event) => setJoinCode(event.target.value.toUpperCase())}
                  placeholder="CODIGO DE SALA"
                />

                <label className="setup-label" htmlFor="join-name">Tu nombre</label>
                <input
                  id="join-name"
                  className="setup-input"
                  type="text"
                  value={joinName}
                  onChange={(event) => setJoinName(event.target.value)}
                  placeholder="Ej: Erik"
                />

                <button type="submit" className="secondary-action multiplayer-join-action" disabled={isBusy}>
                  {isBusy ? 'UNIENDOME...' : 'INTRODUCIR CODIGO'}
                </button>
              </form>
            </article>
          </section>

          <section className="multiplayer-select-badges" aria-label="Indicadores de servicio">
            <div>
              <span className="material-symbols-outlined">security</span>
              <span>Servidores seguros</span>
            </div>
            <div>
              <span className="material-symbols-outlined">bolt</span>
              <span>Conexion global</span>
            </div>
            <div>
              <span className="material-symbols-outlined">groups</span>
              <span>Multiplayer</span>
            </div>
          </section>

          {error ? <p className="error-banner multiplayer-select-error">{error}</p> : null}
        </main>
      </div>
    );
  }

  if (!game && room) {
    const canStart = room.currentPlayers >= room.minimumPlayersToStart;
    const isHostViewer = playerAlias.trim().toLowerCase() === room.hostName.trim().toLowerCase();
    const slotsCount = Math.max(room.minimumPlayersToStart, room.participants.length);
    const slots = Array.from({ length: slotsCount }, (_, index) => room.participants[index] ?? null);

    return (
      <div className="arena-shell multiplayer-lobby-shell">
        <div className="arena-vignette" aria-hidden="true" />
        <div className="arena-radar" aria-hidden="true" />

        <header className="topbar">
          <h1 className="brand">FLIP7</h1>
          <button type="button" className="secondary-action back-nav-action" onClick={handleExitToHome}>
            <span className="material-symbols-outlined">arrow_back</span>
            <span>Volver</span>
          </button>
        </header>

        <main className="multiplayer-lobby-main">
          <section className="multiplayer-lobby-card" aria-label="Sala de espera multijugador">
            <p className="eyebrow multiplayer-lobby-eyebrow">Lobby multijugador</p>

            <button
              type="button"
              className="multiplayer-lobby-code"
              onClick={() => void handleCopyRoomCode(room.code)}
              title="Copiar código de sala"
            >
              {copiedRoomCode ? 'COPIADO' : `SALA ${room.code}`}
            </button>

            <p className="multiplayer-lobby-summary">
              Jugadores: <span>{room.currentPlayers}/{room.minimumPlayersToStart} minimo</span>
              <span className="multiplayer-lobby-dot">•</span>
              Host: <strong>{room.hostName}</strong>
            </p>

            <div className="multiplayer-lobby-grid">
              {slots.map((participant, index) => {
                if (!participant) {
                  return (
                    <div key={`slot-empty-${index}`} className="multiplayer-lobby-slot multiplayer-lobby-slot-empty">
                      <div className="multiplayer-lobby-avatar multiplayer-lobby-avatar-empty">
                        <span className="material-symbols-outlined">person_add</span>
                      </div>
                      <div>
                        <p>Buscando jugador...</p>
                        <div className="multiplayer-lobby-search-line" aria-hidden="true" />
                      </div>
                    </div>
                  );
                }

                const isHost = participant.name === room.hostName;
                const isMe = participant.name === playerAlias;

                return (
                  <div key={participant.id} className="multiplayer-lobby-slot multiplayer-lobby-slot-filled">
                    <div className="multiplayer-lobby-avatar">
                      <span className="material-symbols-outlined">person</span>
                    </div>
                    <div className="multiplayer-lobby-player-meta">
                      <div className="multiplayer-lobby-player-tags">
                        <p>{participant.name.toUpperCase()}</p>
                        {isMe ? <span>TU</span> : null}
                      </div>
                      <small>{isHost ? 'Host conectado' : 'Jugador conectado'}</small>
                    </div>
                    <div className="multiplayer-lobby-presence" aria-label="Conectado" />
                  </div>
                );
              })}
            </div>

            <div className="multiplayer-lobby-actions">
              <button
                type="button"
                className="primary-action multiplayer-lobby-start"
                onClick={() => void startRoom()}
                disabled={isBusy || !canStart || !isHostViewer}
              >
                <span className="material-symbols-outlined">play_arrow</span>
                <span>
                  {isBusy
                    ? 'INICIANDO...'
                    : isHostViewer
                      ? 'INICIAR PARTIDA'
                      : 'ESPERANDO A QUE EL ADMIN DE LA SALA INICIE EL JUEGO'}
                </span>
              </button>
            </div>

            {!canStart ? <p className="multiplayer-lobby-hint">Se requieren al menos 4 jugadores para iniciar.</p> : null}
            {!isHostViewer ? <p className="multiplayer-lobby-hint">Solo el host puede iniciar la partida.</p> : null}
            {error ? <p className="error-banner multiplayer-lobby-error">{error}</p> : null}
          </section>

          <section className="multiplayer-select-badges" aria-label="Indicadores de servicio">
            <div>
              <span className="material-symbols-outlined">security</span>
              <span>Servidores seguros</span>
            </div>
            <div>
              <span className="material-symbols-outlined">bolt</span>
              <span>Conexion global</span>
            </div>
            <div>
              <span className="material-symbols-outlined">groups</span>
              <span>Multiplayer</span>
            </div>
          </section>
        </main>
      </div>
    );
  }

  const activeGame = game!;

  return (
    <div className="arena-shell">
      <div className="arena-vignette" aria-hidden="true" />
      <div className="arena-radar" aria-hidden="true" />

      <header className="topbar">
        <h1 className="brand">FLIP7</h1>
        <div className="topbar-meta-controls">
          <div className="table-meta">
            <span className="eyebrow">ID de mesa</span>
            <span className="meta-value">#{String(activeGame.gameId).slice(0, 6).toUpperCase()}</span>
          </div>
          <button type="button" className="secondary-action back-nav-action" onClick={handleExitToHome}>
            <span className="material-symbols-outlined">logout</span>
            <span>Salir de la partida</span>
          </button>
        </div>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}

      <main className="game-grid">
        <PlayerSidebar players={activeGame.players} currentPlayerId={activeGame.currentTurnPlayerId} />

        <GameTable game={activeGame} activePlayer={viewerPlayer} latestCardId={latestCardId} duplicateFlash={false} />

        <aside className="sidebar sidebar-right">
          {canAct || isPlaywrightTestMode ? (
            <ActionPanel
              disabled={isBusy}
              canAct={canAct}
              onDraw={() => {
                if (canAct && viewerPlayer) {
                  void drawCard(viewerPlayer.id);
                }
              }}
              onStand={() => {
                if (canAct && viewerPlayer) {
                  void stand(viewerPlayer.id);
                }
              }}
            />
          ) : (
            <section className="panel waiting-turn-panel" aria-live="polite">
              <h2 className="panel-title">Comandos de interfaz</h2>
              <div className="waiting-turn-copy">
                <span className="material-symbols-outlined">hourglass_top</span>
                <p>
                  Espera a que sea tu turno.
                  {currentTurnPlayerName ? ` Ahora juega ${currentTurnPlayerName}.` : ''}
                </p>
              </div>
            </section>
          )}

          <EventLog events={activeGame.events} viewerName={playerAlias} />
        </aside>
      </main>

      <footer className="footer">
        <div className="footer-left">
          <div className="metric-line">
            <span className="eyebrow">Ronda</span>
            <span className="metric-inline is-primary">{String(activeGame.currentRound).padStart(2, '0')}</span>
          </div>
          <span className="divider" aria-hidden="true" />
          <div className="metric-line">
            <span className="eyebrow">Cartas restantes</span>
            <span className="metric-inline">{activeGame.deck.length}</span>
          </div>
        </div>

        <div className="footer-right">
          <div className="latency">
            <span className="material-symbols-outlined">sensors</span>
            <span>Servidor activo</span>
          </div>
          <p className="copyright">© 2026 FLIP7 TCG</p>
        </div>
      </footer>

      <RoundSummaryModal game={activeGame} open={activeGame.gamePhase === 'roundSummary'} onNextRound={() => void nextRound()} />
      <WinnerModal game={activeGame} open={activeGame.gamePhase === 'winner'} onRestart={() => void restartGame()} />
      <DuplicateCardAlert alert={activeGame.duplicateAlert} onClose={() => dismissDuplicateAlert()} />
    </div>
  );
}