import { useNavigate } from 'react-router-dom';

import { useGameStore } from '../store/gameStore';

export function Home() {
  const navigate = useNavigate();
  const restartGame = useGameStore((state) => state.restartGame);

  function handleGoToMultiplayer() {
    void restartGame();
    navigate('/multiplayer');
  }

  function handleGoTo1vsIA() {
    void restartGame();
    navigate('/vs-ai');
  }

  return (
    <div className="home-screen">
      <header className="home-topbar">
        <div className="home-logo">FLIP7</div>
        <div className="home-topbar-actions">
          <button type="button" className="home-icon-btn" aria-label="Perfil">
            <span className="material-symbols-outlined">account_circle</span>
          </button>
          <button type="button" className="home-icon-btn" aria-label="Configuración">
            <span className="material-symbols-outlined">settings</span>
          </button>
        </div>
      </header>

      <main className="home-main">
        <div className="home-spotlight" aria-hidden="true" />
        <div className="home-scanline" aria-hidden="true" />

        <section className="home-hero">
          <h1>ENTRA EN LA ARENA</h1>
          <p>ESTRATEGIA DE CARTAS DE LIGA PROFESIONAL</p>
        </section>

        <section className="home-grid" aria-label="Modos de juego">
          <article className="home-card home-card-primary">
            <div className="home-card-icon" aria-hidden="true">
              <span className="material-symbols-outlined">hub</span>
            </div>

            <div className="home-card-content">
              <span className="home-pill home-pill-primary">COMPETITIVO</span>
              <h2>MULTIJUGADOR</h2>
              <p>
                Asciende en las clasificaciones globales en duelos tacticos en tiempo real. Prueba tu mazo contra los
                estrategas mas selectos del mundo.
              </p>
            </div>

            <div className="home-card-footer">
              <div className="home-feature home-feature-primary">
                <span className="material-symbols-outlined home-fill-icon">military_tech</span>
                <span>Ranking Global Próximamente Disponible</span>
              </div>
              <button type="button" className="home-cta home-cta-primary" onClick={handleGoToMultiplayer}>
                INICIAR PARTIDA
              </button>
            </div>
          </article>

          <article className="home-card home-card-secondary">
            <div className="home-card-icon" aria-hidden="true">
              <span className="material-symbols-outlined">memory</span>
            </div>

            <div className="home-card-content">
              <span className="home-pill home-pill-secondary">ENTRENAMIENTO</span>
              <h2>PRACTICA CON IA</h2>
              <p>
                Refina tu estrategia sin riesgos. Enfrentate a modelos adaptativos disenados para imitar tendencias de
                jugadores profesionales.
              </p>
            </div>

            <div className="home-card-footer">
              <div className="home-feature home-feature-secondary">
                <span className="material-symbols-outlined">model_training</span>
                <span>Partida 1 vs IA local</span>
              </div>
              <button type="button" className="home-cta home-cta-secondary" onClick={handleGoTo1vsIA}>
                INICIAR PARTIDA
              </button>
            </div>
          </article>
        </section>

        <section className="home-ticker" aria-label="Estado del sistema">
          <div className="home-ticker-track">
            <span>• MODO MULTIJUGADOR DISPONIBLE</span>
            <span>• NUEVOS PAQUETES DE CARTAS NEON OVERLOAD DISPONIBLES</span>
            <span>• MODO VS IA DISPONIBLE CON OLLAMA</span>
            <span>• NUEVOS CARTAS ESPECIALES DISPONIBLES</span>
          </div>
        </section>
      </main>

      <footer className="home-footer">
        <div className="home-footer-brand">
          <div>FLIP7</div>
          <p>© 2026 FLIP7 DIGITAL. ASCIENDE EN EL TABLERO.</p>
        </div>
        <nav className="home-footer-nav" aria-label="Enlaces de pie de pagina">
          <a href="#" onClick={(event) => event.preventDefault()}>COMUNIDAD</a>
          <a href="#" onClick={(event) => event.preventDefault()}>CLASIFICACIONES</a>
          <a href="#" onClick={(event) => event.preventDefault()}>SOPORTE</a>
          <a href="#" onClick={(event) => event.preventDefault()}>LEGAL</a>
        </nav>
      </footer>
    </div>
  );
}
