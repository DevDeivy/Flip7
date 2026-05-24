import { useGameStore } from '../store/gameStore';

export function useGameState() {
  return useGameStore((state) => state.game);
}
