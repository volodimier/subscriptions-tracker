import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  formatCurrency,
  formatDate,
  formatDateISO,
  formatDateTime,
  daysUntil,
  getBillingCycleLabel,
} from './formatters'

describe('formatters', () => {
  describe('formatCurrency', () => {
    it('should format USD amounts with currency code suffix', () => {
      expect(formatCurrency(9.99, 'USD')).toBe('9.99 USD')
      expect(formatCurrency(100, 'USD')).toBe('100.00 USD')
      expect(formatCurrency(0, 'USD')).toBe('0.00 USD')
    })

    it('should format EUR amounts with currency code suffix', () => {
      expect(formatCurrency(19.99, 'EUR')).toBe('19.99 EUR')
      expect(formatCurrency(50, 'EUR')).toBe('50.00 EUR')
    })

    it('should format GBP amounts with currency code suffix', () => {
      expect(formatCurrency(29.99, 'GBP')).toBe('29.99 GBP')
      expect(formatCurrency(75, 'GBP')).toBe('75.00 GBP')
    })

    it('should format PLN amounts with currency code suffix', () => {
      expect(formatCurrency(49.99, 'PLN')).toBe('49.99 PLN')
      expect(formatCurrency(200, 'PLN')).toBe('200.00 PLN')
    })

    it('should work with any currency code', () => {
      expect(formatCurrency(99.99, 'JPY')).toBe('99.99 JPY')
      expect(formatCurrency(50, 'CHF')).toBe('50.00 CHF')
      expect(formatCurrency(100, 'CAD')).toBe('100.00 CAD')
    })

    it('should handle decimal precision correctly', () => {
      expect(formatCurrency(9.999, 'USD')).toBe('10.00 USD')
      expect(formatCurrency(9.994, 'USD')).toBe('9.99 USD')
      expect(formatCurrency(0.01, 'USD')).toBe('0.01 USD')
      expect(formatCurrency(0.001, 'USD')).toBe('0.00 USD')
    })

    it('should handle negative amounts', () => {
      expect(formatCurrency(-9.99, 'USD')).toBe('-9.99 USD')
      expect(formatCurrency(-100, 'EUR')).toBe('-100.00 EUR')
    })

    it('should handle large amounts', () => {
      expect(formatCurrency(1000000, 'USD')).toBe('1000000.00 USD')
      expect(formatCurrency(99999.99, 'EUR')).toBe('99999.99 EUR')
    })
  })

  describe('formatDate', () => {
    it('should format date strings in US locale', () => {
      expect(formatDate('2025-01-15')).toBe('Jan 15, 2025')
      expect(formatDate('2025-12-31')).toBe('Dec 31, 2025')
      expect(formatDate('2024-06-01')).toBe('Jun 1, 2024')
    })

    it('should handle ISO date-time strings', () => {
      expect(formatDate('2025-01-15T10:30:00Z')).toMatch(/Jan 1[45], 2025/)
    })

    it('should handle different months', () => {
      expect(formatDate('2025-02-28')).toBe('Feb 28, 2025')
      expect(formatDate('2025-03-15')).toBe('Mar 15, 2025')
      expect(formatDate('2025-07-04')).toBe('Jul 4, 2025')
      expect(formatDate('2025-11-25')).toBe('Nov 25, 2025')
    })
  })

  describe('formatDateISO', () => {
    it('should format Date objects to ISO date strings', () => {
      // Use local date constructor to avoid timezone issues
      const date = new Date(2025, 0, 15) // January 15, 2025
      expect(formatDateISO(date)).toBe('2025-01-15')
    })

    it('should handle different dates', () => {
      expect(formatDateISO(new Date(2024, 11, 31))).toBe('2024-12-31') // December 31, 2024
      expect(formatDateISO(new Date(2025, 5, 1))).toBe('2025-06-01') // June 1, 2025
    })

    it('should pad single digit months and days', () => {
      expect(formatDateISO(new Date(2025, 0, 5))).toBe('2025-01-05') // January 5
      expect(formatDateISO(new Date(2025, 8, 9))).toBe('2025-09-09') // September 9
    })

    it('should preserve local date (not UTC-shifted)', () => {
      // Create a date at 11 PM local time - with UTC conversion this could shift to next day
      const latePM = new Date(2025, 0, 15, 23, 30, 0) // Jan 15, 2025 at 11:30 PM local
      expect(formatDateISO(latePM)).toBe('2025-01-15')

      // Create a date at 1 AM local time - with UTC conversion this could shift to previous day
      const earlyAM = new Date(2025, 0, 15, 1, 30, 0) // Jan 15, 2025 at 1:30 AM local
      expect(formatDateISO(earlyAM)).toBe('2025-01-15')
    })

    it('should work at midnight local time', () => {
      const midnight = new Date(2025, 5, 20, 0, 0, 0) // June 20, 2025 at midnight
      expect(formatDateISO(midnight)).toBe('2025-06-20')
    })

    it('should work for dates at year boundaries', () => {
      // New Year's Eve
      const newYearsEve = new Date(2025, 11, 31) // December 31, 2025
      expect(formatDateISO(newYearsEve)).toBe('2025-12-31')

      // New Year's Day
      const newYearsDay = new Date(2026, 0, 1) // January 1, 2026
      expect(formatDateISO(newYearsDay)).toBe('2026-01-01')
    })

    it('should work for dates at month boundaries', () => {
      // Last day of January
      const endJan = new Date(2025, 0, 31)
      expect(formatDateISO(endJan)).toBe('2025-01-31')

      // First day of February
      const startFeb = new Date(2025, 1, 1)
      expect(formatDateISO(startFeb)).toBe('2025-02-01')
    })

    it('should work for leap year dates', () => {
      // Feb 29 on a leap year (2024)
      const leapDay = new Date(2024, 1, 29)
      expect(formatDateISO(leapDay)).toBe('2024-02-29')

      // Feb 28 on a non-leap year (2025)
      const nonLeapFeb28 = new Date(2025, 1, 28)
      expect(formatDateISO(nonLeapFeb28)).toBe('2025-02-28')
    })

    it('should handle dates far in the past and future', () => {
      const pastDate = new Date(2000, 0, 1)
      expect(formatDateISO(pastDate)).toBe('2000-01-01')

      const futureDate = new Date(2099, 11, 31)
      expect(formatDateISO(futureDate)).toBe('2099-12-31')
    })
  })

  describe('formatDateTime', () => {
    it('should format date-time strings with time in 24h format', () => {
      const result = formatDateTime('2025-01-15T14:30:00Z')
      expect(result).toMatch(/1[45] Jan 2025/)
      expect(result).toMatch(/\d{2}:\d{2}/)
      expect(result).not.toMatch(/AM|PM/i)
    })

    it('should include hours and minutes in 24h format', () => {
      const result = formatDateTime('2025-06-20T09:15:00Z')
      expect(result).toMatch(/(19|20) Jun 2025/)
      expect(result).toMatch(/\d{2}:\d{2}/)
    })
  })

  describe('daysUntil', () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('should return 0 for today', () => {
      vi.setSystemTime(new Date('2025-01-15T12:00:00'))
      expect(daysUntil('2025-01-15')).toBe(0)
    })

    it('should return positive days for future dates', () => {
      vi.setSystemTime(new Date('2025-01-15T12:00:00'))
      expect(daysUntil('2025-01-16')).toBe(1)
      expect(daysUntil('2025-01-20')).toBe(5)
      expect(daysUntil('2025-01-22')).toBe(7)
      expect(daysUntil('2025-02-14')).toBe(30)
    })

    it('should return negative days for past dates', () => {
      vi.setSystemTime(new Date('2025-01-15T12:00:00'))
      expect(daysUntil('2025-01-14')).toBe(-1)
      expect(daysUntil('2025-01-10')).toBe(-5)
      expect(daysUntil('2025-01-01')).toBe(-14)
    })

    it('should handle dates across month boundaries', () => {
      vi.setSystemTime(new Date('2025-01-30T12:00:00'))
      expect(daysUntil('2025-02-01')).toBe(2)
    })

    it('should handle dates across year boundaries', () => {
      vi.setSystemTime(new Date('2024-12-31T12:00:00'))
      expect(daysUntil('2025-01-01')).toBe(1)
    })

    it('should handle leap years', () => {
      vi.setSystemTime(new Date('2024-02-28T12:00:00'))
      expect(daysUntil('2024-02-29')).toBe(1)
      expect(daysUntil('2024-03-01')).toBe(2)
    })

    it('should ignore time component when calculating days', () => {
      vi.setSystemTime(new Date('2025-01-15T23:59:59'))
      expect(daysUntil('2025-01-16')).toBe(1)

      vi.setSystemTime(new Date('2025-01-15T00:00:01'))
      expect(daysUntil('2025-01-16')).toBe(1)
    })
  })

  describe('getBillingCycleLabel', () => {
    it('should return correct labels for known cycles', () => {
      expect(getBillingCycleLabel('monthly')).toBe('Monthly')
      expect(getBillingCycleLabel('yearly')).toBe('Yearly')
      expect(getBillingCycleLabel('bi_annual')).toBe('Bi-Annual')
      expect(getBillingCycleLabel('custom')).toBe('Custom')
    })

    it('should return the cycle itself for unknown cycles', () => {
      expect(getBillingCycleLabel('weekly')).toBe('weekly')
      expect(getBillingCycleLabel('quarterly')).toBe('quarterly')
      expect(getBillingCycleLabel('unknown')).toBe('unknown')
      expect(getBillingCycleLabel('')).toBe('')
    })
  })
})
