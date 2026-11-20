// Typed fetch helpers for learning-service (/api) and insights-service (/api/insights).

export interface Item {
  id: string
  title: string
  minutes: number
  completedAt?: string
}

export interface Stage {
  id: string
  title: string
  items: Item[]
}

export interface Roadmap {
  id: string
  learnerId: string
  zoneId: string
  title: string
  progressPercent: number
  stages: Stage[]
}

export interface Card {
  id: string
  roadmapId: string
  itemId?: string
  front: string
  back: string
  repetitions: number
  easeFactor: number
  intervalDays: number
  dueOn: string
}

export interface DayValue {
  day: string
  value: number
}

export interface Retention {
  bucket: string
  reviews: number
  recalled: number
  percent?: number
}

export interface Stats {
  learnerId: string
  today: string
  totalReviews: number
  itemsCompleted: number
  streak: { current: number; longest: number; studiedToday: boolean }
  weeklyMinutes: DayValue[]
  retention: Retention[]
  forecast: DayValue[]
}

export interface NewRoadmap {
  learnerId: string
  zoneId: string
  title: string
  stages: { title: string; items: { title: string; minutes: number }[] }[]
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  if (!response.ok) {
    let detail = response.statusText
    try {
      const problem = (await response.json()) as { detail?: string }
      detail = problem.detail ?? detail
    } catch {
      // not a problem+json body; keep the status text
    }
    throw new Error(detail)
  }
  return (await response.json()) as T
}

const post = <T>(path: string, body?: unknown) =>
  request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })

export const api = {
  roadmaps: (learnerId: string) =>
    request<Roadmap[]>(`/api/learners/${encodeURIComponent(learnerId)}/roadmaps`),
  createRoadmap: (input: NewRoadmap) => post<Roadmap>('/api/roadmaps', input),
  completeItem: (roadmapId: string, itemId: string) =>
    post<Roadmap>(`/api/roadmaps/${roadmapId}/items/${itemId}/complete`),
  cards: (roadmapId: string) => request<Card[]>(`/api/roadmaps/${roadmapId}/cards`),
  addCard: (roadmapId: string, card: { itemId?: string; front: string; back: string }) =>
    post<Card>(`/api/roadmaps/${roadmapId}/cards`, card),
  due: (learnerId: string) => request<Card[]>(`/api/learners/${encodeURIComponent(learnerId)}/due`),
  review: (cardId: string, grade: number, durationSeconds: number) =>
    post<{ reviewId: string; card: Card }>(`/api/cards/${cardId}/reviews`, { grade, durationSeconds }),
  stats: (learnerId: string) =>
    request<Stats>(`/api/insights/learners/${encodeURIComponent(learnerId)}/stats`),
  statsStreamUrl: (learnerId: string) =>
    `/api/insights/learners/${encodeURIComponent(learnerId)}/stream`,
}
