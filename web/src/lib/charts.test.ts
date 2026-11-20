import { describe, expect, it } from 'vitest'
import { bars, weekday } from './charts'

describe('bars', () => {
  it('scales the tallest bar to the full height and keeps zero bars flat', () => {
    const laid = bars(
      [
        { label: 'a', value: 10 },
        { label: 'b', value: 5 },
        { label: 'c', value: 0 },
      ],
      100,
      50,
      5,
    )
    expect(laid[0].height).toBe(50)
    expect(laid[1].height).toBe(25)
    expect(laid[2].height).toBe(0)
    expect(laid[2].x + laid[2].width).toBeCloseTo(100)
  })

  it('handles all-zero data without dividing by zero', () => {
    expect(bars([{ label: 'a', value: 0 }], 10, 10).every((b) => Number.isFinite(b.height))).toBe(true)
  })
})

describe('weekday', () => {
  it('reads ISO dates as calendar dates', () => {
    expect(weekday('2026-03-02')).toBe('Mon')
  })
})
