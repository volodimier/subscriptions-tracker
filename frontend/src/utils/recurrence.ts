export type RecurrenceCadence = 'monthly' | 'yearly'

interface DateParts {
  year: number
  month: number
  day: number
}

function parseDateOnly(value: string): DateParts | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return null
  }
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) {
    return null
  }

  const maxDay = daysInMonth(year, month)
  if (month < 1 || month > 12 || day < 1 || day > maxDay) {
    return null
  }

  return { year, month, day }
}

function formatDateOnly(parts: DateParts): string {
  return `${parts.year}-${String(parts.month).padStart(2, '0')}-${String(parts.day).padStart(2, '0')}`
}

function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate()
}

function toDateAtUtcMidnight(value: string): Date {
  const parsed = parseDateOnly(value)
  if (!parsed) {
    throw new Error(`Invalid date-only value: ${value}`)
  }
  return new Date(Date.UTC(parsed.year, parsed.month - 1, parsed.day))
}

function subtractDays(value: string, days: number): string {
  const date = toDateAtUtcMidnight(value)
  date.setUTCDate(date.getUTCDate() - days)
  return formatDateOnly({
    year: date.getUTCFullYear(),
    month: date.getUTCMonth() + 1,
    day: date.getUTCDate(),
  })
}

function getLocalDateTimePartsInZone(now: Date, userTimeZone: string): {
  date: string
  hour: number
  minute: number
} {
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: userTimeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    hourCycle: 'h23',
  })
  const parts = formatter.formatToParts(now)
  const get = (type: string) => Number(parts.find(part => part.type === type)?.value)

  const year = get('year')
  const month = get('month')
  const day = get('day')
  const hour = get('hour')
  const minute = get('minute')

  if (!year || !month || !day || Number.isNaN(hour) || Number.isNaN(minute)) {
    throw new Error('Failed to resolve local date-time in timezone')
  }

  return {
    date: formatDateOnly({ year, month, day }),
    hour: hour % 24,
    minute,
  }
}

export function isDateOnly(value: string): boolean {
  return parseDateOnly(value) !== null
}

export function computeUserCutoffDate(userTimeZone: string, now: Date = new Date()): string {
  if (!userTimeZone?.trim()) {
    throw new Error('User timezone is required')
  }

  const local = getLocalDateTimePartsInZone(now, userTimeZone)
  const beforeCutoff = local.hour === 0 && local.minute < 5
  return beforeCutoff ? subtractDays(local.date, 1) : local.date
}

export function getMonthlyAnchorOptions(nextBillingDate: string): number[] | null {
  const parsed = parseDateOnly(nextBillingDate)
  if (!parsed) {
    return null
  }

  if (parsed.month === 2 && parsed.day === 28) {
    return [28, 29, 30, 31]
  }
  if (parsed.month === 2 && parsed.day === 29) {
    return [29, 30, 31]
  }
  if (parsed.day === 30) {
    return [30, 31]
  }
  return null
}

export function getYearlyAnchorOptions(nextBillingDate: string): string[] | null {
  const parsed = parseDateOnly(nextBillingDate)
  if (!parsed) {
    return null
  }
  if (parsed.month === 2 && parsed.day === 28) {
    return ['02-28', '02-29']
  }
  return null
}

export function advanceWithAnchor(
  date: string,
  cadence: RecurrenceCadence,
  anchorDay: number,
  anchorMonth?: number
): string {
  const parsed = parseDateOnly(date)
  if (!parsed) {
    throw new Error(`Invalid date-only value: ${date}`)
  }
  if (anchorDay < 1 || anchorDay > 31) {
    throw new Error(`Invalid anchor day: ${anchorDay}`)
  }

  if (cadence === 'monthly') {
    const targetMonthIndex = parsed.month
    const targetYear = targetMonthIndex === 12 ? parsed.year + 1 : parsed.year
    const targetMonth = targetMonthIndex === 12 ? 1 : targetMonthIndex + 1
    const day = Math.min(anchorDay, daysInMonth(targetYear, targetMonth))
    return formatDateOnly({ year: targetYear, month: targetMonth, day })
  }

  if (!anchorMonth || anchorMonth < 1 || anchorMonth > 12) {
    throw new Error(`Invalid anchor month for yearly cadence: ${anchorMonth}`)
  }

  const targetYear = parsed.year + 1
  const day = Math.min(anchorDay, daysInMonth(targetYear, anchorMonth))
  return formatDateOnly({ year: targetYear, month: anchorMonth, day })
}

export function generateNextOccurrences(
  start: string,
  cadence: RecurrenceCadence,
  anchorDay: number,
  anchorMonth?: number,
  count = 3
): string[] {
  if (!isDateOnly(start) || count <= 0) {
    return []
  }

  const occurrences = [start]
  while (occurrences.length < count) {
    occurrences.push(advanceWithAnchor(occurrences[occurrences.length - 1], cadence, anchorDay, anchorMonth))
  }
  return occurrences
}

export function computeExpectedNextAfterCutoff(
  firstBillingDate: string,
  cutoffDate: string,
  cadence: RecurrenceCadence,
  anchorDay: number,
  anchorMonth?: number
): string {
  if (!isDateOnly(firstBillingDate) || !isDateOnly(cutoffDate)) {
    throw new Error('Expected first billing date and cutoff date in YYYY-MM-DD format')
  }

  let occurrence = firstBillingDate
  while (occurrence <= cutoffDate) {
    occurrence = advanceWithAnchor(occurrence, cadence, anchorDay, anchorMonth)
  }
  return occurrence
}
