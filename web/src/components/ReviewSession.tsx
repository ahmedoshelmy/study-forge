import { useEffect, useState } from 'react'
import type { Card } from '../lib/api'
import { api } from '../lib/api'
import { GRADES, describeInterval, reviewSeconds } from '../lib/review'

interface Props {
  learnerId: string
  refreshKey: number
  onReviewed: () => void
  onError: (message: string) => void
}

/** "Review now": one due card at a time, flip to see the answer, grade 0-5. */
export function ReviewSession({ learnerId, refreshKey, onReviewed, onError }: Props) {
  const [due, setDue] = useState<Card[]>([])
  const [flipped, setFlipped] = useState(false)
  const [shownAt, setShownAt] = useState(() => Date.now())
  const [last, setLast] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    api
      .due(learnerId)
      .then((cards) => {
        if (!cancelled) {
          setDue(cards)
          setFlipped(false)
          setShownAt(Date.now())
        }
      })
      .catch((e: Error) => onError(e.message))
    return () => {
      cancelled = true
    }
  }, [learnerId, refreshKey, onError])

  const card = due[0]

  async function grade(value: number) {
    if (!card) return
    try {
      const result = await api.review(card.id, value, reviewSeconds(shownAt, Date.now()))
      setLast(`Next review ${describeInterval(result.card.intervalDays)}`)
      setDue((rest) => rest.slice(1))
      setFlipped(false)
      setShownAt(Date.now())
      onReviewed()
    } catch (e) {
      onError((e as Error).message)
    }
  }

  return (
    <section className="panel review">
      <header className="panel-head">
        <h2>Review now</h2>
        <span className="muted">{due.length} due</span>
      </header>
      {card ? (
        <>
          <button type="button" className={`flash ${flipped ? 'flipped' : ''}`} onClick={() => setFlipped((f) => !f)}>
            <span className="flash-side">{flipped ? card.back : card.front}</span>
            <span className="muted small">{flipped ? 'answer' : 'tap to show the answer'}</span>
          </button>
          <div className="grades" aria-label="Grade your recall">
            {GRADES.map((g) => (
              <button key={g.grade} type="button" disabled={!flipped} title={g.hint} className={`grade g${g.grade}`} onClick={() => void grade(g.grade)}>
                <strong>{g.grade}</strong> {g.label}
              </button>
            ))}
          </div>
        </>
      ) : (
        <p className="empty">Nothing due. Come back tomorrow, or add cards to a roadmap.</p>
      )}
      {last && <p className="toast" role="status">{last}</p>}
    </section>
  )
}
