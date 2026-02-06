<script setup lang="ts">
import { AlertTriangle } from 'lucide-vue-next'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

defineProps<{
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'danger' | 'warning' | 'info'
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

function handleConfirm() {
  emit('confirm')
}

function handleCancel() {
  emit('cancel')
}
</script>

<template>
  <Dialog :open="true">
    <DialogContent class="sm:max-w-md" @escape-key-down="handleCancel" @pointer-down-outside="handleCancel">
      <DialogHeader>
        <div class="flex items-start gap-4">
          <div
            :class="[
              'flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full',
              {
                'bg-red-100': type === 'danger',
                'bg-yellow-100': type === 'warning',
                'bg-blue-100': type === 'info' || !type,
              },
            ]"
          >
            <AlertTriangle
              :class="[
                'h-5 w-5',
                {
                  'text-red-600': type === 'danger',
                  'text-yellow-600': type === 'warning',
                  'text-blue-600': type === 'info' || !type,
                },
              ]"
            />
          </div>
          <div class="flex flex-col gap-1.5">
            <DialogTitle>{{ title }}</DialogTitle>
            <DialogDescription>{{ message }}</DialogDescription>
          </div>
        </div>
      </DialogHeader>
      <DialogFooter class="mt-4">
        <Button variant="outline" @click="handleCancel">
          {{ cancelText || 'Cancel' }}
        </Button>
        <Button
          :variant="type === 'danger' ? 'destructive' : 'default'"
          :class="{ 'bg-yellow-600 hover:bg-yellow-500': type === 'warning' }"
          @click="handleConfirm"
        >
          {{ confirmText || 'Confirm' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
