import { describe, it, expect } from 'vitest'
import { computeTimeline } from './historyDiff'
import type { SubscriptionHistoryEntry } from '@/types'

const createEntry = (overrides: Partial<SubscriptionHistoryEntry> = {}): SubscriptionHistoryEntry => ({
  id: 1,
  subscriptionId: 42,
  changedAt: '2026-02-08T12:00:00Z',
  amount: 9.99,
  currencyCode: 'USD',
  paymentMethod: 'Credit Card',
  notes: undefined,
  ...overrides,
})

describe('computeTimeline', () => {
  it('should return empty array for empty input', () => {
    expect(computeTimeline([])).toEqual([])
  })

  it('should mark a single entry as "Subscription created"', () => {
    const entries = [createEntry({ changedAt: '2026-01-01T00:00:00Z' })]
    const result = computeTimeline(entries)

    expect(result).toHaveLength(1)
    expect(result[0].date).toBe('2026-01-01T00:00:00Z')
    expect(result[0].changes).toEqual(['Subscription created'])
  })

  it('should detect price changes between consecutive snapshots', () => {
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', amount: 14.99 }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', amount: 9.99 }),
    ]
    const result = computeTimeline(entries)

    expect(result).toHaveLength(2)
    expect(result[0].changes).toEqual(['Price changed from 9.99 USD to 14.99 USD'])
    expect(result[1].changes).toEqual(['Subscription created'])
  })

  it('should detect currency changes', () => {
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', amount: 9.99, currencyCode: 'EUR' }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', amount: 9.99, currencyCode: 'USD' }),
    ]
    const result = computeTimeline(entries)

    expect(result[0].changes).toEqual(['Price changed from 9.99 USD to 9.99 EUR'])
  })

  it('should detect payment method changes', () => {
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', paymentMethod: 'Mastercard' }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', paymentMethod: 'Visa' }),
    ]
    const result = computeTimeline(entries)

    expect(result[0].changes).toEqual(['Payment method changed from Visa to Mastercard'])
  })

  it('should detect payment method added from none', () => {
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', paymentMethod: 'Visa' }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', paymentMethod: undefined }),
    ]
    const result = computeTimeline(entries)

    expect(result[0].changes).toEqual(['Payment method changed from none to Visa'])
  })

  it('should detect payment method removed', () => {
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', paymentMethod: undefined }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', paymentMethod: 'Visa' }),
    ]
    const result = computeTimeline(entries)

    expect(result[0].changes).toEqual(['Payment method changed from Visa to none'])
  })

  it('should detect notes added', () => {
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', notes: 'Family plan' }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', notes: undefined }),
    ]
    const result = computeTimeline(entries)

    expect(result[0].changes).toEqual(['Notes added'])
  })

  it('should detect notes removed', () => {
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', notes: undefined }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', notes: 'Family plan' }),
    ]
    const result = computeTimeline(entries)

    expect(result[0].changes).toEqual(['Notes removed'])
  })

  it('should detect notes updated', () => {
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', notes: 'Premium plan' }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', notes: 'Family plan' }),
    ]
    const result = computeTimeline(entries)

    expect(result[0].changes).toEqual(['Notes updated'])
  })

  it('should detect multiple changes in one snapshot', () => {
    const entries = [
      createEntry({
        id: 2,
        changedAt: '2026-02-08T12:00:00Z',
        amount: 19.99,
        paymentMethod: 'PayPal',
        notes: 'Upgraded',
      }),
      createEntry({
        id: 1,
        changedAt: '2026-01-01T00:00:00Z',
        amount: 9.99,
        paymentMethod: 'Visa',
        notes: undefined,
      }),
    ]
    const result = computeTimeline(entries)

    expect(result[0].changes).toHaveLength(3)
    expect(result[0].changes).toContain('Price changed from 9.99 USD to 19.99 USD')
    expect(result[0].changes).toContain('Payment method changed from Visa to PayPal')
    expect(result[0].changes).toContain('Notes added')
  })

  it('should handle a timeline with three entries correctly', () => {
    const entries = [
      createEntry({ id: 3, changedAt: '2026-03-01T00:00:00Z', amount: 19.99, paymentMethod: 'Mastercard' }),
      createEntry({ id: 2, changedAt: '2026-02-01T00:00:00Z', amount: 14.99, paymentMethod: 'Visa' }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', amount: 9.99, paymentMethod: 'Visa' }),
    ]
    const result = computeTimeline(entries)

    expect(result).toHaveLength(3)

    // Most recent: price and payment method changed
    expect(result[0].date).toBe('2026-03-01T00:00:00Z')
    expect(result[0].changes).toContain('Price changed from 14.99 USD to 19.99 USD')
    expect(result[0].changes).toContain('Payment method changed from Visa to Mastercard')

    // Middle: only price changed
    expect(result[1].date).toBe('2026-02-01T00:00:00Z')
    expect(result[1].changes).toEqual(['Price changed from 9.99 USD to 14.99 USD'])

    // Earliest: creation
    expect(result[2].date).toBe('2026-01-01T00:00:00Z')
    expect(result[2].changes).toEqual(['Subscription created'])
  })

  it('should show "Subscription updated" when no fields differ', () => {
    // This can happen if only non-tracked fields changed or due to data edge cases
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z' }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z' }),
    ]
    const result = computeTimeline(entries)

    expect(result[0].changes).toEqual(['Subscription updated'])
  })

  it('should treat empty string notes same as undefined', () => {
    const entries = [
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', notes: '' }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', notes: undefined }),
    ]
    const result = computeTimeline(entries)

    // Empty string and undefined are both falsy, so no notes change detected
    expect(result[0].changes).toEqual(['Subscription updated'])
  })
})
