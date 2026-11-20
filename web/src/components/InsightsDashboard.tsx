import { useEffect, useState } from 'react'
import type { Stats } from '../lib/api'
import { api } from '../lib/api'
import { weekday } from '../lib/charts'
import { BarChart } from './BarChart'

/** Live stats from insights-service: initial GET, then Server-Sent Events after every projected event. */
export function InsightsDashboard({ learnerId }: { learnerId: string }) {
  const [stats, setStats] = useState<Stats | null>(null)
  const [live, setLive] = useState(false)

  useEffect(() => {
    api.stats(learnerId).then(setStats).catch(() => setStats(null))
    const source = new EventSource(api.statsStreamUrl(learnerId))
    source.addEventListener('stats', (e) => setStats(JSON.parse((e as MessageEvent<string>).data) as Stats))
    source.onopen = () => setLive(true)
    source.onerror = () => setLive(false)
    return () => source.close()
  }, [learnerId])

  if (!stats) return <section className="panel"><p className="empty">Insights are loading…</p></section>

  return (
    <section className="panel insights">
      <header className="panel-head">
        <h2>Insights</h2>
        <span className={`live ${live ? 'on' : ''}`}>{live ? 'live' : 'offline'}</span>
      </header>
      <div className="tiles">
        <div className="tile">
          <span className="tile-value">{stats.streak.current}</span>
          <span className="tile-label">day streak{stats.streak.studiedToday ? '' : ' (study today to keep it)'}</span>
        </div>
        <div className="tile">
          <span className="tile-value">{stats.streak.longest}</span>
          <span className="tile-label">longest streak</span>
        </div>
        <div className="tile">
          <span className="tile-value" data-testid="total-reviews">{stats.totalReviews}</span>
          <span className="tile-label">reviews</span>
        </div>
        <div className="tile">
          <span className="tile-value">{stats.itemsCompleted}</span>
          <span className="tile-label">items done</span>
        </div>
      </div>
      <div className="charts">
        <BarChart title="Minutes this week" unit="min" data={stats.weeklyMinutes.map((d) => ({ label: weekday(d.day), value: d.value }))} />
        <BarChart title="Cards due, next 14 days" unit="cards" data={stats.forecast.map((d) => ({ label: d.day.slice(8), value: d.value }))} />
        <BarChart
          title="Recall rate by interval (%)"
          unit="%"
          data={stats.retention.map((r) => ({ label: r.bucket, value: r.percent ?? 0 }))}
        />
      </div>
    </section>
  )
}
