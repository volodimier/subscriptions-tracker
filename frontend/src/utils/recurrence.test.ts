import { describe, it, expect } from 'vitest'
import {
  isDateOnly,
  computeUserCutoffDate,
  getMonthlyAnchorOptions,
  getYearlyAnchorOptions,
  advanceWithAnchor,
  generateNextOccurrences,
  computeExpectedNextAfterCutoff,
} from './recurrence'

describe('recurrence utils', () => {
  describe('isDateOnly', () => {
    it('accepts valid date-only values', () => {
      expect(isDateOnly('2026-02-28')).toBe(true)
      expect(isDateOnly('2024-02-29')).toBe(true)
    })

    it('rejects invalid values', () => {
      expect(isDateOnly('2026-02-30')).toBe(false)
      expect(isDateOnly('2026-2-28')).toBe(false)
      expect(isDateOnly('')).toBe(false)
    })
  })

  describe('computeUserCutoffDate', () => {
    it('uses previous date before 00:05 local time', () => {
      const now = new Date('2026-02-10T00:03:00Z')
      expect(computeUserCutoffDate('UTC', now)).toBe('2026-02-09')
    })

    it('uses local date at or after 00:05 local time', () => {
      const now = new Date('2026-02-10T00:05:00Z')
      expect(computeUserCutoffDate('UTC', now)).toBe('2026-02-10')
    })
  })

  describe('anchor ambiguity options', () => {
    it('returns monthly ambiguity options', () => {
      expect(getMonthlyAnchorOptions('2026-02-28')).toEqual([28, 29, 30, 31])
      expect(getMonthlyAnchorOptions('2024-02-29')).toEqual([29, 30, 31])
      expect(getMonthlyAnchorOptions('2026-04-30')).toEqual([30, 31])
      expect(getMonthlyAnchorOptions('2026-05-31')).toBeNull()
      expect(getMonthlyAnchorOptions('2026-03-12')).toBeNull()
    })

    it('returns yearly ambiguity options', () => {
      expect(getYearlyAnchorOptions('2026-02-28')).toEqual(['02-28', '02-29'])
      expect(getYearlyAnchorOptions('2024-02-29')).toBeNull()
      expect(getYearlyAnchorOptions('2026-07-04')).toBeNull()
    })
  })

  describe('advanceWithAnchor', () => {
    it('applies monthly clamp without drift', () => {
      const feb = advanceWithAnchor('2025-01-31', 'monthly', 31)
      const mar = advanceWithAnchor(feb, 'monthly', 31)

      expect(feb).toBe('2025-02-28')
      expect(mar).toBe('2025-03-31')
    })

    it('applies yearly leap clamp for Feb-29 anchor', () => {
      const next = advanceWithAnchor('2024-02-29', 'yearly', 29, 2)
      expect(next).toBe('2025-02-28')
    })
  })

  describe('generateNextOccurrences', () => {
    it('returns requested number of monthly occurrences including start', () => {
      expect(generateNextOccurrences('2026-02-28', 'monthly', 31, undefined, 3)).toEqual([
        '2026-02-28',
        '2026-03-31',
        '2026-04-30',
      ])
    })
  })

  describe('computeExpectedNextAfterCutoff', () => {
    it('matches strict monthly schedule example', () => {
      expect(
        computeExpectedNextAfterCutoff('2025-11-30', '2026-02-09', 'monthly', 30)
      ).toBe('2026-02-28')
    })

    it('handles yearly Feb-29 anchor progression', () => {
      expect(
        computeExpectedNextAfterCutoff('2024-02-29', '2025-03-01', 'yearly', 29, 2)
      ).toBe('2026-02-28')
    })
  })
})
