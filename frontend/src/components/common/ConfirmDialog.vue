<script setup lang="ts">
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
</script>

<template>
  <div class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex min-h-full items-center justify-center p-4">
      <div class="fixed inset-0 bg-gray-900/60 backdrop-blur-sm transition-opacity" @click="emit('cancel')"></div>

      <div class="relative transform overflow-hidden rounded-2xl bg-white text-left shadow-soft-lg transition-all sm:w-full sm:max-w-lg">
        <div class="bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
          <div class="sm:flex sm:items-start">
            <div
              :class="[
                'mx-auto flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full sm:mx-0 sm:h-10 sm:w-10',
                {
                  'bg-red-100': type === 'danger',
                  'bg-yellow-100': type === 'warning',
                  'bg-blue-100': type === 'info' || !type,
                },
              ]"
            >
              <svg
                :class="[
                  'h-6 w-6',
                  {
                    'text-red-600': type === 'danger',
                    'text-yellow-600': type === 'warning',
                    'text-blue-600': type === 'info' || !type,
                  },
                ]"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"
                />
              </svg>
            </div>
            <div class="mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left">
              <h3 class="text-base font-semibold leading-6 text-gray-900">{{ title }}</h3>
              <div class="mt-2">
                <p class="text-sm text-gray-500">{{ message }}</p>
              </div>
            </div>
          </div>
        </div>
        <div class="bg-gray-50/80 px-4 py-4 sm:flex sm:flex-row-reverse sm:px-6">
          <button
            type="button"
            :class="[
              'inline-flex w-full justify-center rounded-lg px-4 py-2.5 text-sm font-medium text-white shadow-sm transition-colors sm:ml-3 sm:w-auto',
              {
                'bg-red-600 hover:bg-red-500': type === 'danger',
                'bg-yellow-600 hover:bg-yellow-500': type === 'warning',
                'bg-primary-600 hover:bg-primary-500': type === 'info' || !type,
              },
            ]"
            @click="emit('confirm')"
          >
            {{ confirmText || 'Confirm' }}
          </button>
          <button
            type="button"
            class="mt-3 inline-flex w-full justify-center rounded-lg bg-white px-4 py-2.5 text-sm font-medium text-gray-700 shadow-sm ring-1 ring-inset ring-gray-200 hover:bg-gray-50 transition-colors sm:mt-0 sm:w-auto"
            @click="emit('cancel')"
          >
            {{ cancelText || 'Cancel' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
