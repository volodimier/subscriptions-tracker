export const CURRENCIES = ['USD', 'EUR', 'GBP', 'PLN'] as const
export type Currency = typeof CURRENCIES[number]

export const BILLING_CYCLES = [
  { value: 'monthly', label: 'Monthly' },
  { value: 'yearly', label: 'Yearly' },
  { value: 'bi_annual', label: 'Bi-Annual (6 months)' },
  { value: 'custom', label: 'Custom' },
] as const

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
