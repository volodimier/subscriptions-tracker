import type { BillingCycle } from '@/types'

function formatLocalDateISO(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function calculateNextBillingDate(
  startDate: string,
  billingCycle: BillingCycle,
  customDays?: number
): string {
  if (!startDate) return ''

  const [year, month, day] = startDate.split('-').map(Number)
  const next = new Date(year, month - 1, day)

  switch (billingCycle) {
    case 'monthly':
      next.setMonth(next.getMonth() + 1)
      break
    case 'yearly':
      next.setFullYear(next.getFullYear() + 1)
      break
    case 'bi_annual':
      next.setMonth(next.getMonth() + 6)
      break
    case 'custom': {
      const days = customDays || 30
      next.setDate(next.getDate() + days)
      break
    }
  }

  return formatLocalDateISO(next)
}

/**
 * Calculates the start date by subtracting one billing cycle from the next billing date.
 * This is the inverse of calculateNextBillingDate.
 *
 * @param nextBillingDate - The next billing date in ISO format (YYYY-MM-DD)
 * @param billingCycle - The billing cycle type
 * @param customDays - Number of days for custom billing cycle
 * @returns The calculated start date in ISO format (YYYY-MM-DD)
 */
export function calculateStartDate(
  nextBillingDate: string,
  billingCycle: BillingCycle,
  customDays?: number
): string {
  if (!nextBillingDate) return ''

  const [year, month, day] = nextBillingDate.split('-').map(Number)
  const start = new Date(year, month - 1, day)

  switch (billingCycle) {
    case 'monthly':
      start.setMonth(start.getMonth() - 1)
      break
    case 'yearly':
      start.setFullYear(start.getFullYear() - 1)
      break
    case 'bi_annual':
      start.setMonth(start.getMonth() - 6)
      break
    case 'custom': {
      const days = customDays || 30
      start.setDate(start.getDate() - days)
      break
    }
  }

  return formatLocalDateISO(start)
}
