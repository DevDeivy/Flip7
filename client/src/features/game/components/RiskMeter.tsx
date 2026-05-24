import { getRiskLabel } from '../utils/labels';

interface RiskMeterProps {
  value: number;
  label?: string;
}

export function RiskMeter({ value, label = 'Estabilidad del sistema' }: RiskMeterProps) {
  const tone = value >= 80 ? 'critical' : value >= 60 ? 'warning' : 'stable';
  const stateLabel = getRiskLabel(value);

  return (
    <section className="stability">
      <div className="section-caption">
        <span className="eyebrow">{label}</span>
        <span className={`risk-label ${tone}`}>{stateLabel}</span>
      </div>

      <div className="meter" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={value}>
        <div className="meter-fill" style={{ width: `${Math.max(8, Math.min(value, 100))}%` }} />
      </div>
    </section>
  );
}
