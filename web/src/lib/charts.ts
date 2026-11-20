// Tiny chart math for the hand-drawn SVG charts (no chart library needed).

export interface Bar {
  x: number
  y: number
  width: number
  height: number
  value: number
  label: string
}

/** Lays values out as bars in a width x height box with a gap between bars. */
export function bars(
  values: { label: string; value: number }[],
  width: number,
  height: number,
  gap = 4,
): Bar[] {
  if (values.length === 0) return []
  const max = Math.max(1, ...values.map((v) => v.value))
  const barWidth = (width - gap * (values.length - 1)) / values.length
  return values.map((v, i) => {
    const h = (Math.max(0, v.value) / max) * height
    return { x: i * (barWidth + gap), y: height - h, width: barWidth, height: h, value: v.value, label: v.label }
  })
}

/** Short weekday label for an ISO date (parsed as a calendar date, never shifted by time zone). */
export function weekday(isoDate: string): string {
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d)).toLocaleDateString('en', { weekday: 'short', timeZone: 'UTC' })
}
