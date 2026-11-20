import { bars } from '../lib/charts'

interface Props {
  title: string
  data: { label: string; value: number }[]
  unit: string
  height?: number
}

/** Accessible SVG bar chart: each bar has a title, and the values are also listed for screen readers. */
export function BarChart({ title, data, unit, height = 120 }: Props) {
  const width = 320
  const laid = bars(data, width, height, 6)
  return (
    <figure className="chart">
      <figcaption>{title}</figcaption>
      <svg viewBox={`0 0 ${width} ${height + 18}`} role="img" aria-label={title}>
        {laid.map((b) => (
          <g key={b.label}>
            <rect x={b.x} y={b.y} width={b.width} height={Math.max(b.height, 1)} rx={3} className="bar">
              <title>{`${b.label}: ${b.value} ${unit}`}</title>
            </rect>
            {b.value > 0 && (
              <text x={b.x + b.width / 2} y={Math.max(b.y - 3, 10)} className="bar-value" textAnchor="middle">
                {b.value}
              </text>
            )}
            <text x={b.x + b.width / 2} y={height + 14} className="bar-label" textAnchor="middle">
              {b.label}
            </text>
          </g>
        ))}
      </svg>
    </figure>
  )
}
