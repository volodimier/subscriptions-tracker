import { describe, it, expect } from 'vitest'
import { BILLING_CYCLES, SELECTABLE_BILLING_CYCLES, CURRENCIES } from './constants'

describe('constants', () => {
  describe('BILLING_CYCLES', () => {
    it('should contain all billing cycle options', () => {
      expect(BILLING_CYCLES).toHaveLength(4)
      expect(BILLING_CYCLES.map(c => c.value)).toEqual(['monthly', 'yearly', 'bi_annual', 'custom'])
    })

    it('should have labels for all cycles', () => {
      const cycleWithLabels = BILLING_CYCLES.filter(c => c.label)
      expect(cycleWithLabels).toHaveLength(4)
    })
  })

  describe('SELECTABLE_BILLING_CYCLES', () => {
    it('should only contain monthly and yearly options', () => {
      expect(SELECTABLE_BILLING_CYCLES).toHaveLength(2)
      expect(SELECTABLE_BILLING_CYCLES.map(c => c.value)).toEqual(['monthly', 'yearly'])
    })

    it('should not contain bi_annual or custom options', () => {
      const values = SELECTABLE_BILLING_CYCLES.map(c => c.value)
      expect(values).not.toContain('bi_annual')
      expect(values).not.toContain('custom')
    })

    it('should be a subset of BILLING_CYCLES', () => {
      const allValues = BILLING_CYCLES.map(c => c.value)
      const selectableValues = SELECTABLE_BILLING_CYCLES.map(c => c.value)
      selectableValues.forEach(value => {
        expect(allValues).toContain(value)
      })
    })

    it('should have matching labels with BILLING_CYCLES', () => {
      SELECTABLE_BILLING_CYCLES.forEach(selectable => {
        const matching = BILLING_CYCLES.find(c => c.value === selectable.value)
        expect(matching).toBeDefined()
        expect(matching?.label).toBe(selectable.label)
      })
    })
  })

  describe('CURRENCIES', () => {
    it('should contain supported currencies', () => {
      expect(CURRENCIES).toContain('USD')
      expect(CURRENCIES).toContain('EUR')
      expect(CURRENCIES).toContain('GBP')
      expect(CURRENCIES).toContain('PLN')
    })
  })
})
