interface ActionPanelProps {
  disabled: boolean;
  canAct: boolean;
  onDraw: () => void;
  onStand: () => void;
  actionLabel?: string;
}

export function ActionPanel({ disabled, canAct, onDraw, onStand, actionLabel = 'Comandos de interfaz' }: ActionPanelProps) {
  return (
    <section className="panel actions-panel">
      <h2 className="panel-title">{actionLabel}</h2>

      <div className="actions">
        <button
          type="button"
          className="primary-action"
          onClick={onDraw}
          disabled={disabled || !canAct}
          data-testid="draw-button"
        >
          <span className="material-symbols-outlined">style</span>
          <span>Robar carta</span>
        </button>

        <button
          type="button"
          className="secondary-action"
          onClick={onStand}
          disabled={disabled || !canAct}
          data-testid="stand-button"
        >
          <span className="material-symbols-outlined">pan_tool</span>
          <span>Plantarse</span>
        </button>
      </div>
    </section>
  );
}
