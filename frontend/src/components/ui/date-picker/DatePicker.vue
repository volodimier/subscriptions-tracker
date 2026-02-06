<script setup lang="ts">
import { computed, ref } from 'vue'
import { CalendarDate, type DateValue, getLocalTimeZone, today } from '@internationalized/date'
import { CalendarIcon } from 'lucide-vue-next'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'

const props = defineProps<{
  placeholder?: string
  disabled?: boolean
  class?: string
}>()

// v-model with ISO date string (YYYY-MM-DD)
const modelValue = defineModel<string>({ default: '' })

const isOpen = ref(false)

// Convert ISO string to DateValue for the calendar
const calendarValue = computed<DateValue | undefined>({
  get() {
    if (!modelValue.value) return undefined
    const [year, month, day] = modelValue.value.split('-').map(Number)
    if (!year || !month || !day) return undefined
    return new CalendarDate(year, month, day)
  },
  set(value: DateValue | undefined) {
    if (value) {
      // Convert DateValue back to ISO string
      const year = value.year
      const month = String(value.month).padStart(2, '0')
      const day = String(value.day).padStart(2, '0')
      modelValue.value = `${year}-${month}-${day}`
      isOpen.value = false
    } else {
      modelValue.value = ''
    }
  }
})

// Format the date for display
const displayDate = computed(() => {
  if (!modelValue.value) return ''
  const date = new Date(modelValue.value + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
})

const defaultDate = today(getLocalTimeZone())
</script>

<template>
  <Popover v-model:open="isOpen">
    <PopoverTrigger as-child>
      <Button
        variant="outline"
        :disabled="disabled"
        :class="cn(
          'w-full justify-start text-left font-normal',
          !modelValue && 'text-muted-foreground',
          props.class
        )"
      >
        <CalendarIcon class="mr-2 h-4 w-4" />
        {{ displayDate || placeholder || 'Pick a date' }}
      </Button>
    </PopoverTrigger>
    <PopoverContent class="w-auto p-0" align="start">
      <Calendar
        v-model="calendarValue"
        :default-value="defaultDate"
        initial-focus
      />
    </PopoverContent>
  </Popover>
</template>
