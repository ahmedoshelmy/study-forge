import { useCallback, useEffect, useState } from 'react'
import type { Card, Roadmap } from './lib/api'
import { api } from './lib/api'
import { SAMPLE_CARDS, sampleRoadmap } from './lib/sample'
import { InsightsDashboard } from './components/InsightsDashboard'
import { ReviewSession } from './components/ReviewSession'
import { RoadmapPanel } from './components/RoadmapPanel'

const LEARNER_KEY = 'study-forge.learner'

function initialLearner(): string {
  try {
    return localStorage.getItem(LEARNER_KEY) ?? 'demo-learner'
  } catch {
    return 'demo-learner'
  }
}

export default function App() {
  const [learnerId, setLearnerId] = useState(initialLearner)
  const [roadmaps, setRoadmaps] = useState<Roadmap[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [cards, setCards] = useState<Card[]>([])
  const [refreshKey, setRefreshKey] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), [])
  const onError = useCallback((message: string) => setError(message), [])

  useEffect(() => {
    try {
      localStorage.setItem(LEARNER_KEY, learnerId)
    } catch {
      // storage blocked; the learner id just isn't remembered
    }
    api
      .roadmaps(learnerId)
      .then((list) => {
        setRoadmaps(list)
        setSelected((current) => (current && list.some((r) => r.id === current) ? current : (list[0]?.id ?? null)))
      })
      .catch((e: Error) => setError(e.message))
  }, [learnerId, refreshKey])

  useEffect(() => {
    if (!selected) return
    api.cards(selected).then(setCards).catch((e: Error) => setError(e.message))
  }, [selected, refreshKey])

  async function createSample() {
    try {
      const zone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
      const roadmap = await api.createRoadmap(sampleRoadmap(learnerId, zone))
      for (const card of SAMPLE_CARDS) {
        await api.addCard(roadmap.id, card)
      }
      setSelected(roadmap.id)
      refresh()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  const roadmap = roadmaps.find((r) => r.id === selected)

  return (
    <main>
      <header className="top">
        <div>
          <h1>Study Forge</h1>
          <p className="muted">Roadmaps, spaced repetition and live progress.</p>
        </div>
        <label className="learner">
          Learner
          <input value={learnerId} onChange={(e) => setLearnerId(e.target.value.trim() || 'demo-learner')} />
        </label>
      </header>
      {error && (
        <p className="error" role="alert">
          {error} <button type="button" onClick={() => setError(null)}>Dismiss</button>
        </p>
      )}
      <nav className="roadmap-tabs" aria-label="Roadmaps">
        {roadmaps.map((r) => (
          <button key={r.id} type="button" className={r.id === selected ? 'active' : ''} onClick={() => setSelected(r.id)}>
            {r.title} <span className="muted">{r.progressPercent}%</span>
          </button>
        ))}
        <button type="button" className="ghost" onClick={() => void createSample()}>
          + Sample roadmap
        </button>
      </nav>
      <div className="layout">
        <div className="column">
          <ReviewSession learnerId={learnerId} refreshKey={refreshKey} onReviewed={refresh} onError={onError} />
          {roadmap ? (
            <RoadmapPanel roadmap={roadmap} cards={cards.filter((c) => c.roadmapId === roadmap.id)} onChanged={refresh} onError={onError} />
          ) : (
            <section className="panel">
              <p className="empty">No roadmap yet. Start with the sample one.</p>
            </section>
          )}
        </div>
        <div className="column">
          <InsightsDashboard learnerId={learnerId} />
        </div>
      </div>
    </main>
  )
}
