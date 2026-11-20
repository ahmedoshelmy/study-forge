import { useState } from 'react'
import type { Card, Roadmap } from '../lib/api'
import { api } from '../lib/api'

interface Props {
  roadmap: Roadmap
  cards: Card[]
  onChanged: () => void
  onError: (message: string) => void
}

export function RoadmapPanel({ roadmap, cards, onChanged, onError }: Props) {
  const [front, setFront] = useState('')
  const [back, setBack] = useState('')

  async function complete(itemId: string) {
    try {
      await api.completeItem(roadmap.id, itemId)
      onChanged()
    } catch (e) {
      onError((e as Error).message)
    }
  }

  async function addCard(event: React.FormEvent) {
    event.preventDefault()
    try {
      await api.addCard(roadmap.id, { front, back })
      setFront('')
      setBack('')
      onChanged()
    } catch (e) {
      onError((e as Error).message)
    }
  }

  return (
    <section className="panel">
      <header className="panel-head">
        <h2>{roadmap.title}</h2>
        <span className="muted">{roadmap.zoneId}</span>
      </header>
      <div className="progress" role="progressbar" aria-valuenow={roadmap.progressPercent} aria-valuemin={0} aria-valuemax={100}>
        <div className="progress-fill" style={{ width: `${roadmap.progressPercent}%` }} />
      </div>
      <p className="muted">{roadmap.progressPercent}% done</p>
      {roadmap.stages.map((stage) => (
        <div key={stage.id} className="stage">
          <h3>{stage.title}</h3>
          <ul className="items">
            {stage.items.map((item) => (
              <li key={item.id} className={item.completedAt ? 'done' : ''}>
                <label>
                  <input
                    type="checkbox"
                    checked={Boolean(item.completedAt)}
                    disabled={Boolean(item.completedAt)}
                    onChange={() => void complete(item.id)}
                  />
                  <span>{item.title}</span>
                </label>
                <span className="muted">{item.minutes} min</span>
              </li>
            ))}
          </ul>
        </div>
      ))}
      <h3>Cards ({cards.length})</h3>
      <form className="card-form" onSubmit={(e) => void addCard(e)}>
        <input aria-label="Question" placeholder="Question" value={front} onChange={(e) => setFront(e.target.value)} required />
        <input aria-label="Answer" placeholder="Answer" value={back} onChange={(e) => setBack(e.target.value)} required />
        <button type="submit">Add card</button>
      </form>
      <ul className="card-list">
        {cards.map((card) => (
          <li key={card.id}>
            <span>{card.front}</span>
            <span className="muted">due {card.dueOn}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}
