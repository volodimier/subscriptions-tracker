export function formatCurrency(amount: number, currencyCode: string): string {
  return `${amount.toFixed(2)} ${currencyCode}`
}

export function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

export function formatDateISO(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function formatDateTime(dateString: string): string {
  // Ensure the date string is treated as UTC if no timezone specified
  const normalized = dateString.endsWith('Z') || dateString.includes('+') || dateString.includes('-', 10)
    ? dateString
    : dateString + 'Z'
  const date = new Date(normalized)
  return date.toLocaleString('en-GB', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

export function daysUntil(dateString: string): number {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const targetDate = new Date(dateString)
  targetDate.setHours(0, 0, 0, 0)
  const diff = targetDate.getTime() - today.getTime()
  return Math.ceil(diff / (1000 * 60 * 60 * 24))
}

export function getBillingCycleLabel(cycle: string): string {
  const labels: Record<string, string> = {
    monthly: 'Monthly',
    yearly: 'Yearly',
    bi_annual: 'Bi-Annual',
    custom: 'Custom',
  }
  return labels[cycle] || cycle
}
