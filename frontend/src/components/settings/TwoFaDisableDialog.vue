<script setup lang="ts">
import { ref, computed } from 'vue'
import { ShieldOff, Loader2 } from 'lucide-vue-next'
import { twoFaService } from '@/services/twoFaService'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const password = ref('')
const totpCode = ref('')
const error = ref('')
const loading = ref(false)

const isOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})

const isValid = computed(() => {
  return password.value.trim() !== '' && totpCode.value.length === 6
})

async function handleDisable() {
  if (!isValid.value) return

  loading.value = true
  error.value = ''
  try {
    await twoFaService.disable(password.value, totpCode.value)
    resetState()
    emit('success')
    isOpen.value = false
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    error.value = axiosError.response?.data?.message || 'Failed to disable 2FA'
  } finally {
    loading.value = false
  }
}

function resetState() {
  password.value = ''
  totpCode.value = ''
  error.value = ''
  loading.value = false
}

function handleClose() {
  resetState()
  isOpen.value = false
}

function handleCodeInput(event: Event) {
  const input = event.target as HTMLInputElement
  // Only allow numeric input
  input.value = input.value.replace(/\D/g, '').slice(0, 6)
  totpCode.value = input.value
}
</script>

<template>
  <Dialog :open="isOpen" @update:open="(value) => { if (!value) handleClose() }">
    <DialogContent class="sm:max-w-md" @escape-key-down="handleClose" @pointer-down-outside="handleClose">
      <DialogHeader>
        <DialogTitle class="flex items-center gap-2 text-red-600">
          <ShieldOff class="h-5 w-5" />
          Disable Two-Factor Authentication
        </DialogTitle>
        <DialogDescription>
          This will remove two-factor authentication from your account. You will no longer need a code to sign in.
        </DialogDescription>
      </DialogHeader>

      <Alert v-if="error" variant="destructive" class="mt-2">
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <form class="space-y-4 py-4" @submit.prevent="handleDisable">
        <div class="space-y-2">
          <Label for="disablePassword">Password</Label>
          <Input
            id="disablePassword"
            v-model="password"
            type="password"
            required
            placeholder="Enter your password"
          />
        </div>

        <div class="space-y-2">
          <Label for="disableTotpCode">Authentication Code</Label>
          <Input
            id="disableTotpCode"
            :value="totpCode"
            type="text"
            inputmode="numeric"
            maxlength="6"
            required
            placeholder="000000"
            class="text-center tracking-widest font-mono"
            @input="handleCodeInput"
          />
          <p class="text-xs text-muted-foreground">
            Enter the 6-digit code from your authenticator app
          </p>
        </div>

        <DialogFooter class="pt-4">
          <Button type="button" variant="outline" @click="handleClose">Cancel</Button>
          <Button type="submit" variant="destructive" :disabled="!isValid || loading">
            <Loader2 v-if="loading" class="mr-2 h-4 w-4 animate-spin" />
            Disable 2FA
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
</template>
