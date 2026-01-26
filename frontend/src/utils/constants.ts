export const CURRENCIES = ['USD', 'EUR', 'GBP', 'PLN'] as const
export type Currency = typeof CURRENCIES[number]

export const BILLING_CYCLES = [
  { value: 'monthly', label: 'Monthly' },
  { value: 'yearly', label: 'Yearly' },
  { value: 'bi_annual', label: 'Bi-Annual (6 months)' },
  { value: 'custom', label: 'Custom' },
] as const

export const CURRENCY_SYMBOLS: Record<string, string> = {
  USD: '$',
  EUR: '\u20AC',
  GBP: '\u00A3',
  PLN: 'z\u0142',
}

export const CATEGORY_SUGGESTIONS = [
  'Entertainment',
  'Media',
  'Music',
  'Streaming',
  'Fitness',
  'Health',
  'Software',
  'Storage',
  'News',
  'Education',
  'Gaming',
  'Productivity',
  'Business',
  'Other',
]
