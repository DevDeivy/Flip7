import { Route, Routes } from 'react-router-dom';

import { Home } from './features/game/pages/Home';
import { MultiplayerPage } from './features/game/pages/MultiplayerPage';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/multiplayer" element={<MultiplayerPage />} />
      <Route path="/vs-ai" element={<MultiplayerPage />} />

    </Routes>
  );
}

export default App;
