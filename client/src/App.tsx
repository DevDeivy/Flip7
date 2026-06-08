import { Route, Routes } from 'react-router-dom';

import { Home } from './features/game/pages/Home';
import { MultiplayerPage } from './features/game/pages/MultiplayerPage';
import { VsAIPage } from './features/game/pages/vsAI';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/multiplayer" element={<MultiplayerPage />} />
      <Route path="/vs-ai" element={<VsAIPage />} />

    </Routes>
  );
}

export default App;
