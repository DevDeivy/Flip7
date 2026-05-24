import { useGameStore } from '../store/gameStore';
import { useShallow } from 'zustand/react/shallow';

export function useGameActions() {
  return useGameStore(
    useShallow((state) => ({
      initializeGame: state.initializeGame,
      drawCard: state.drawCard,
      stand: state.stand,
      nextRound: state.nextRound,
      restartGame: state.restartGame,
      dismissDuplicateAlert: state.dismissDuplicateAlert,
    })),
  );
}
