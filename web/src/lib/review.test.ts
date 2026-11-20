import { describe, expect, it } from 'vitest'
import { GRADES, describeInterval, reviewSeconds } from './review'

describe('review helpers', () => {
  it('offers the six SM-2 grades in order', () => {
    expect(GRADES.map((g) => g.grade)).toEqual([0, 1, 2, 3, 4, 5])
  })

  it('clamps review time to 0..3600 seconds', () => {
    expect(reviewSeconds(1_000, 13_400)).toBe(12)
    expect(reviewSeconds(5_000, 1_000)).toBe(0)
    expect(reviewSeconds(0, 10_000_000)).toBe(3600)
  })

  it('describes the next interval', () => {
    expect(describeInterval(1)).toBe('tomorrow')
    expect(describeInterval(6)).toBe('in 6 days')
  })
})
