// A synthetic starter roadmap so a fresh install has something to click through.
import type { NewRoadmap } from './api'

export function sampleRoadmap(learnerId: string, zoneId: string): NewRoadmap {
  return {
    learnerId,
    zoneId,
    title: 'Event streaming in four weeks',
    stages: [
      {
        title: 'Basics',
        items: [
          { title: 'Topics, partitions and offsets', minutes: 30 },
          { title: 'Consumer groups and rebalancing', minutes: 45 },
        ],
      },
      {
        title: 'Reliability',
        items: [
          { title: 'Idempotent producer and acks', minutes: 30 },
          { title: 'Retry topics and dead letters', minutes: 40 },
          { title: 'Transactional outbox', minutes: 50 },
        ],
      },
    ],
  }
}

export const SAMPLE_CARDS: { front: string; back: string }[] = [
  { front: 'What keeps one learner’s events in order?', back: 'Using the learner id as the record key: one key, one partition, one order.' },
  { front: 'Why make the producer idempotent?', back: 'Broker de-duplicates retried batches, so a retry never writes a record twice.' },
  { front: 'What does a dead letter topic hold?', back: 'Messages that failed every retry (or can never succeed), parked for a human.' },
  { front: 'Why a processed_events table on the consumer?', back: 'Delivery is at least once; storing event ids makes applying an event idempotent.' },
]
