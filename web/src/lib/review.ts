// Review helpers: SM-2 grade labels and how long a card was looked at.

export interface GradeOption {
  grade: number
  label: string
  hint: string
}

export const GRADES: readonly GradeOption[] = [
  { grade: 0, label: 'Blank', hint: 'No idea at all' },
  { grade: 1, label: 'Wrong', hint: 'Wrong, but it rang a bell' },
  { grade: 2, label: 'Almost', hint: 'Wrong, but easy once shown' },
  { grade: 3, label: 'Hard', hint: 'Right, with real effort' },
  { grade: 4, label: 'Good', hint: 'Right after a pause' },
  { grade: 5, label: 'Easy', hint: 'Instant and sure' },
]

/** Seconds between showing the card and grading it, clamped to what the API accepts. */
export function reviewSeconds(shownAt: number, gradedAt: number): number {
  const seconds = Math.round((gradedAt - shownAt) / 1000)
  return Math.min(3600, Math.max(0, seconds))
}

/** "in 1 day", "in 6 days", "today" for the toast after grading. */
export function describeInterval(days: number): string {
  if (days <= 0) return 'today'
  return days === 1 ? 'tomorrow' : `in ${days} days`
}
