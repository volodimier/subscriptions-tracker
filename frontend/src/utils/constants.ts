export const CURRENCIES = ['USD', 'EUR', 'GBP', 'PLN'] as const
export type Currency = typeof CURRENCIES[number]

export const BILLING_CYCLES = [
  { value: 'monthly', label: 'Monthly' },
  { value: 'yearly', label: 'Yearly' },
  { value: 'bi_annual', label: 'Bi-Annual (6 months)' },
  { value: 'custom', label: 'Custom' },
] as const

// Billing cycles available for selection in forms (bi_annual and custom are hidden from UI)
export const SELECTABLE_BILLING_CYCLES = [
  { value: 'monthly', label: 'Monthly' },
  { value: 'yearly', label: 'Yearly' },
] as const

