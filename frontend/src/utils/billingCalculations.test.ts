import { describe, it, expect } from 'vitest'
import { calculateNextBillingDate, calculateStartDate } from './billingCalculations'

describe('billingCalculations', () => {
  describe('calculateNextBillingDate', () => {
    describe('monthly cycle', () => {
      it('should add one month to the start date', () => {
        expect(calculateNextBillingDate('2025-01-15', 'monthly')).toBe('2025-02-15')
        expect(calculateNextBillingDate('2025-06-01', 'monthly')).toBe('2025-07-01')
        expect(calculateNextBillingDate('2025-12-10', 'monthly')).toBe('2026-01-10')
      })

      it('should handle end of month dates', () => {
        expect(calculateNextBillingDate('2025-01-31', 'monthly')).toBe('2025-03-03')
        expect(calculateNextBillingDate('2025-03-31', 'monthly')).toBe('2025-05-01')
      })

      it('should handle February edge cases', () => {
        expect(calculateNextBillingDate('2025-01-29', 'monthly')).toBe('2025-03-01')
        expect(calculateNextBillingDate('2025-01-30', 'monthly')).toBe('2025-03-02')
        expect(calculateNextBillingDate('2025-01-31', 'monthly')).toBe('2025-03-03')
      })

      it('should handle leap year February', () => {
        expect(calculateNextBillingDate('2024-01-29', 'monthly')).toBe('2024-02-29')
        expect(calculateNextBillingDate('2024-01-30', 'monthly')).toBe('2024-03-01')
      })
    })

    describe('yearly cycle', () => {
      it('should add one year to the start date', () => {
        expect(calculateNextBillingDate('2025-01-15', 'yearly')).toBe('2026-01-15')
        expect(calculateNextBillingDate('2025-06-01', 'yearly')).toBe('2026-06-01')
        expect(calculateNextBillingDate('2025-12-31', 'yearly')).toBe('2026-12-31')
      })

      it('should handle leap year February 29', () => {
        expect(calculateNextBillingDate('2024-02-29', 'yearly')).toBe('2025-03-01')
      })

      it('should preserve date across multiple years', () => {
        expect(calculateNextBillingDate('2020-07-04', 'yearly')).toBe('2021-07-04')
      })
    })

    describe('bi_annual cycle', () => {
      it('should add six months to the start date', () => {
        expect(calculateNextBillingDate('2025-01-15', 'bi_annual')).toBe('2025-07-15')
        expect(calculateNextBillingDate('2025-03-01', 'bi_annual')).toBe('2025-09-01')
        expect(calculateNextBillingDate('2025-06-15', 'bi_annual')).toBe('2025-12-15')
      })

      it('should cross year boundary correctly', () => {
        expect(calculateNextBillingDate('2025-07-15', 'bi_annual')).toBe('2026-01-15')
        expect(calculateNextBillingDate('2025-08-01', 'bi_annual')).toBe('2026-02-01')
        expect(calculateNextBillingDate('2025-10-20', 'bi_annual')).toBe('2026-04-20')
      })

      it('should handle end of month dates', () => {
        expect(calculateNextBillingDate('2025-08-31', 'bi_annual')).toBe('2026-03-03')
        expect(calculateNextBillingDate('2025-03-31', 'bi_annual')).toBe('2025-10-01')
      })
    })

    describe('custom cycle', () => {
      it('should add specified days to the start date', () => {
        expect(calculateNextBillingDate('2025-01-15', 'custom', 7)).toBe('2025-01-22')
        expect(calculateNextBillingDate('2025-01-15', 'custom', 14)).toBe('2025-01-29')
        expect(calculateNextBillingDate('2025-01-15', 'custom', 30)).toBe('2025-02-14')
        expect(calculateNextBillingDate('2025-01-15', 'custom', 90)).toBe('2025-04-15')
      })

      it('should default to 30 days when customDays not provided', () => {
        expect(calculateNextBillingDate('2025-01-15', 'custom')).toBe('2025-02-14')
        expect(calculateNextBillingDate('2025-01-15', 'custom', undefined)).toBe('2025-02-14')
      })

      it('should handle 1 day cycle', () => {
        expect(calculateNextBillingDate('2025-01-15', 'custom', 1)).toBe('2025-01-16')
      })

      it('should handle crossing month boundaries', () => {
        expect(calculateNextBillingDate('2025-01-25', 'custom', 10)).toBe('2025-02-04')
        expect(calculateNextBillingDate('2025-02-20', 'custom', 15)).toBe('2025-03-07')
      })

      it('should handle crossing year boundaries', () => {
        expect(calculateNextBillingDate('2025-12-25', 'custom', 10)).toBe('2026-01-04')
        expect(calculateNextBillingDate('2025-12-01', 'custom', 45)).toBe('2026-01-15')
      })

      it('should handle large day values', () => {
        expect(calculateNextBillingDate('2025-01-01', 'custom', 365)).toBe('2026-01-01')
        expect(calculateNextBillingDate('2025-01-01', 'custom', 180)).toBe('2025-06-30')
      })
    })

    describe('edge cases', () => {
      it('should return empty string for empty start date', () => {
        expect(calculateNextBillingDate('', 'monthly')).toBe('')
        expect(calculateNextBillingDate('', 'yearly')).toBe('')
        expect(calculateNextBillingDate('', 'custom', 30)).toBe('')
      })

      it('should handle first day of month', () => {
        expect(calculateNextBillingDate('2025-01-01', 'monthly')).toBe('2025-02-01')
        expect(calculateNextBillingDate('2025-01-01', 'yearly')).toBe('2026-01-01')
        expect(calculateNextBillingDate('2025-01-01', 'bi_annual')).toBe('2025-07-01')
      })

      it('should handle last day of month', () => {
        expect(calculateNextBillingDate('2025-04-30', 'monthly')).toBe('2025-05-30')
        expect(calculateNextBillingDate('2025-06-30', 'monthly')).toBe('2025-07-30')
      })
    })
  })

  describe('calculateStartDate', () => {
    describe('monthly cycle', () => {
      it('should subtract one month from the next billing date', () => {
        expect(calculateStartDate('2025-02-15', 'monthly')).toBe('2025-01-15')
        expect(calculateStartDate('2025-07-01', 'monthly')).toBe('2025-06-01')
        expect(calculateStartDate('2026-01-10', 'monthly')).toBe('2025-12-10')
      })

      it('should handle end of month dates', () => {
        // March 31 - 1 month = February 28 (or 29 in leap year) becomes March 3 due to JS Date behavior
        expect(calculateStartDate('2025-03-31', 'monthly')).toBe('2025-03-03')
        expect(calculateStartDate('2025-05-31', 'monthly')).toBe('2025-05-01')
      })

      it('should handle February edge cases', () => {
        expect(calculateStartDate('2025-03-01', 'monthly')).toBe('2025-02-01')
        expect(calculateStartDate('2025-03-28', 'monthly')).toBe('2025-02-28')
      })

      it('should handle leap year February', () => {
        expect(calculateStartDate('2024-02-29', 'monthly')).toBe('2024-01-29')
        expect(calculateStartDate('2024-03-01', 'monthly')).toBe('2024-02-01')
      })
    })

    describe('yearly cycle', () => {
      it('should subtract one year from the next billing date', () => {
        expect(calculateStartDate('2026-01-15', 'yearly')).toBe('2025-01-15')
        expect(calculateStartDate('2026-06-01', 'yearly')).toBe('2025-06-01')
        expect(calculateStartDate('2026-12-31', 'yearly')).toBe('2025-12-31')
      })

      it('should handle leap year February 29', () => {
        // March 1, 2025 - 1 year = March 1, 2024
        expect(calculateStartDate('2025-03-01', 'yearly')).toBe('2024-03-01')
        // Feb 29, 2024 - 1 year = Feb 28, 2023 (becomes March 1 due to JS Date)
        expect(calculateStartDate('2024-02-29', 'yearly')).toBe('2023-03-01')
      })

      it('should preserve date across multiple years', () => {
        expect(calculateStartDate('2021-07-04', 'yearly')).toBe('2020-07-04')
      })
    })

    describe('bi_annual cycle', () => {
      it('should subtract six months from the next billing date', () => {
        expect(calculateStartDate('2025-07-15', 'bi_annual')).toBe('2025-01-15')
        expect(calculateStartDate('2025-09-01', 'bi_annual')).toBe('2025-03-01')
        expect(calculateStartDate('2025-12-15', 'bi_annual')).toBe('2025-06-15')
      })

      it('should cross year boundary correctly', () => {
        expect(calculateStartDate('2026-01-15', 'bi_annual')).toBe('2025-07-15')
        expect(calculateStartDate('2026-02-01', 'bi_annual')).toBe('2025-08-01')
        expect(calculateStartDate('2026-04-20', 'bi_annual')).toBe('2025-10-20')
      })

      it('should handle end of month dates', () => {
        expect(calculateStartDate('2026-03-31', 'bi_annual')).toBe('2025-10-01')
        expect(calculateStartDate('2025-10-31', 'bi_annual')).toBe('2025-05-01')
      })
    })

    describe('custom cycle', () => {
      it('should subtract specified days from the next billing date', () => {
        expect(calculateStartDate('2025-01-22', 'custom', 7)).toBe('2025-01-15')
        expect(calculateStartDate('2025-01-29', 'custom', 14)).toBe('2025-01-15')
        expect(calculateStartDate('2025-02-14', 'custom', 30)).toBe('2025-01-15')
        expect(calculateStartDate('2025-04-15', 'custom', 90)).toBe('2025-01-15')
      })

      it('should default to 30 days when customDays not provided', () => {
        expect(calculateStartDate('2025-02-14', 'custom')).toBe('2025-01-15')
        expect(calculateStartDate('2025-02-14', 'custom', undefined)).toBe('2025-01-15')
      })

      it('should handle 1 day cycle', () => {
        expect(calculateStartDate('2025-01-16', 'custom', 1)).toBe('2025-01-15')
      })

      it('should handle crossing month boundaries', () => {
        expect(calculateStartDate('2025-02-04', 'custom', 10)).toBe('2025-01-25')
        expect(calculateStartDate('2025-03-07', 'custom', 15)).toBe('2025-02-20')
      })

      it('should handle crossing year boundaries', () => {
        expect(calculateStartDate('2026-01-04', 'custom', 10)).toBe('2025-12-25')
        expect(calculateStartDate('2026-01-15', 'custom', 45)).toBe('2025-12-01')
      })

      it('should handle large day values', () => {
        expect(calculateStartDate('2026-01-01', 'custom', 365)).toBe('2025-01-01')
        expect(calculateStartDate('2025-06-30', 'custom', 180)).toBe('2025-01-01')
      })
    })

    describe('edge cases', () => {
      it('should return empty string for empty next billing date', () => {
        expect(calculateStartDate('', 'monthly')).toBe('')
        expect(calculateStartDate('', 'yearly')).toBe('')
        expect(calculateStartDate('', 'custom', 30)).toBe('')
      })

      it('should handle first day of month', () => {
        expect(calculateStartDate('2025-02-01', 'monthly')).toBe('2025-01-01')
        expect(calculateStartDate('2026-01-01', 'yearly')).toBe('2025-01-01')
        expect(calculateStartDate('2025-07-01', 'bi_annual')).toBe('2025-01-01')
      })

      it('should handle last day of month', () => {
        expect(calculateStartDate('2025-05-30', 'monthly')).toBe('2025-04-30')
        expect(calculateStartDate('2025-07-30', 'monthly')).toBe('2025-06-30')
      })
    })
  })
})
