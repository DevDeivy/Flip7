import { Link } from 'react-router-dom';

export function Home() {
  return (
    <div className="arena-shell flex items-center justify-center">
      <div className="arena-vignette" aria-hidden="true" />
      <div className="arena-radar" aria-hidden="true" />

      <div className="loading-card panel max-w-md w-full text-center py-12">
        <p className="eyebrow mb-2">Diviertete en</p>
        <h1 className="brand mb-8">FLIP7</h1>
        
        <div className="flex flex-col gap-4 mt-6">
          <Link 
            to="/multiplayer" 
            className="panel flex items-center justify-center py-4 px-6 hover:border-primary transition-colors group"
          >
            <div className="flex flex-col items-center">
              <span className="material-symbols-outlined text-primary mb-2 group-hover:scale-110 transition-transform">groups</span>
              <span className="font-bold text-lg">Multiplayer</span>
              <span className="eyebrow text-[0.6rem] mt-1">Partidas en local con otros jugadores</span>
            </div>
          </Link>

          <Link 
            to="/vs-ai" 
            className="panel flex items-center justify-center py-4 px-6 hover:border-warning transition-colors group opacity-80 hover:opacity-100"
          >
            <div className="flex flex-col items-center">
              <span className="material-symbols-outlined text-warning mb-2 group-hover:scale-110 transition-transform">smart_toy</span>
              <span className="font-bold text-lg">1 vs 1 contra IA</span>
              <span className="eyebrow text-[0.6rem] mt-1">Jugar con la IA en tiempo real</span>
            </div>
          </Link>
        </div>
        
        <p className="loading-copy mt-10 text-xs opacity-50 uppercase tracking-widest">
          Aplican terminos y condiciones
        </p>
      </div>
    </div>
  );
}
