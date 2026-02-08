import type { SubscriptionHistoryEntry } from '@/types'
import { formatCurrency } from '@/utils/formatters'

export interface TimelineEntry {
  date: string
  changes: string[]
}

/**
 * Computes a human-readable timeline from a list of subscription history snapshots.
 *
 * The entries are expected in descending order by changedAt (newest first),
 * which is how the API returns them. The last entry in the array (earliest by date)
 * is treated as the initial creation snapshot and displayed as "Subscription created".
 *
 * For all other entries, we diff against the next entry in the array (the previous
 * snapshot chronologically) and describe only what changed.
 */
export function computeTimeline(entries: SubscriptionHistoryEntry[]): TimelineEntry[] {
  if (entries.length === 0) return []

  const timeline: TimelineEntry[] = []

  for (let i = 0; i < entries.length; i++) {
    const current = entries[i]

    // The last entry in the descending list is the earliest (creation snapshot)
    if (i === entries.length - 1) {
      timeline.push({
        date: current.changedAt,
        changes: ['Subscription created'],
      })
      continue
    }

    // Compare current entry against the next one in the array (which is
    // chronologically older, since the list is sorted descending)
    const previous = entries[i + 1]
    const changes: string[] = []

    if (current.amount !== previous.amount || current.currencyCode !== previous.currencyCode) {
      const oldFormatted = formatCurrency(previous.amount, previous.currencyCode)
      const newFormatted = formatCurrency(current.amount, current.currencyCode)
      changes.push(`Price changed from ${oldFormatted} to ${newFormatted}`)
    }

    if ((current.paymentMethod || '') !== (previous.paymentMethod || '')) {
      const oldMethod = previous.paymentMethod || 'none'
      const newMethod = current.paymentMethod || 'none'
      changes.push(`Payment method changed from ${oldMethod} to ${newMethod}`)
    }

    if ((current.notes || '') !== (previous.notes || '')) {
      if (!previous.notes && current.notes) {
        changes.push('Notes added')
      } else if (previous.notes && !current.notes) {
        changes.push('Notes removed')
      } else {
        changes.push('Notes updated')
      }
    }

    // If no detectable changes (shouldn't normally happen), show a generic message
    if (changes.length === 0) {
      changes.push('Subscription updated')
    }

    timeline.push({
      date: current.changedAt,
      changes,
    })
  }

  return timeline
}
