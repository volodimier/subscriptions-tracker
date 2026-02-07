<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Shield, Copy, Download, Loader2, Check } from 'lucide-vue-next'
import { twoFaService } from '@/services/twoFaService'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import {
  Dialog,
  DialogScrollContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import type { TotpSetupResponse } from '@/types'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

// State
const step = ref<'setup' | 'verify' | 'recovery'>('setup')
const setupData = ref<TotpSetupResponse | null>(null)
const verificationCode = ref('')
const recoveryCodes = ref<string[]>([])
const error = ref('')
const loading = ref(false)
const copiedSecret = ref(false)
const copiedCodes = ref(false)
const downloadedCodes = ref(false)
const savedCodesConfirmed = ref(false)

const canConfirmSaved = computed(() => copiedCodes.value || downloadedCodes.value)

const isOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})

// Watch for dialog opening to trigger setup
watch(() => props.open, (newValue) => {
  if (newValue && step.value === 'setup' && !setupData.value && !loading.value) {
    initiateSetup()
  }
}, { immediate: true })

async function initiateSetup() {
  loading.value = true
  error.value = ''
  try {
    setupData.value = await twoFaService.initiateSetup()
    step.value = 'verify'
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    error.value = axiosError.response?.data?.message || 'Failed to initiate 2FA setup'
  } finally {
    loading.value = false
  }
}

async function verifySetup() {
  if (verificationCode.value.length !== 6) {
    error.value = 'Please enter a 6-digit code'
    return
  }

  loading.value = true
  error.value = ''
  try {
    const response = await twoFaService.completeSetup(verificationCode.value)
    recoveryCodes.value = response.recoveryCodes
    step.value = 'recovery'
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    error.value = axiosError.response?.data?.message || 'Invalid verification code'
  } finally {
    loading.value = false
  }
}

async function copySecret() {
  if (setupData.value?.secret) {
    await navigator.clipboard.writeText(setupData.value.secret)
    copiedSecret.value = true
    setTimeout(() => {
      copiedSecret.value = false
    }, 2000)
  }
}

async function copyCodes() {
  const content = recoveryCodes.value.join('\n')
  await navigator.clipboard.writeText(content)
  copiedCodes.value = true
}

function downloadCodes() {
  const content = recoveryCodes.value.join('\n')
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'recovery-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
  downloadedCodes.value = true
}

function handleDone() {
  resetState()
  emit('success')
  isOpen.value = false
}

function resetState() {
  step.value = 'setup'
  setupData.value = null
  verificationCode.value = ''
  recoveryCodes.value = []
  error.value = ''
  loading.value = false
  copiedSecret.value = false
  copiedCodes.value = false
  downloadedCodes.value = false
  savedCodesConfirmed.value = false
}

function handleClose() {
  // Only allow closing if we're on setup step or completed recovery step
  if (step.value === 'setup' || (step.value === 'recovery' && savedCodesConfirmed.value)) {
    resetState()
    isOpen.value = false
  }
}

function handleCodeInput(event: Event) {
  const input = event.target as HTMLInputElement
  // Only allow numeric input
  input.value = input.value.replace(/\D/g, '').slice(0, 6)
  verificationCode.value = input.value
}

// Start setup when dialog opens
function handleOpenChange(open: boolean) {
  if (open && step.value === 'setup' && !setupData.value) {
    initiateSetup()
  }
  if (!open) {
    handleClose()
  }
}
</script>

<template>
  <Dialog :open="isOpen" @update:open="handleOpenChange">
    <DialogScrollContent class="sm:max-w-md" @escape-key-down.prevent @pointer-down-outside.prevent>
      <DialogHeader>
        <DialogTitle class="flex items-center gap-2">
          <Shield class="h-5 w-5" />
          <span v-if="step === 'setup' || step === 'verify'">Set Up Two-Factor Authentication</span>
          <span v-else>Save Your Recovery Codes</span>
        </DialogTitle>
      </DialogHeader>

      <Alert v-if="error" variant="destructive" class="mt-2">
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <!-- Setup Step - Loading -->
      <div v-if="step === 'setup'" class="py-4">
        <div class="flex items-center justify-center py-8">
          <Loader2 class="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
        <p class="text-center text-sm text-muted-foreground">Setting up two-factor authentication...</p>
      </div>

      <!-- Verify Step - QR Code and Code Input -->
      <div v-else-if="step === 'verify'" class="py-4 space-y-6">
        <div class="space-y-4">
          <p class="text-sm text-muted-foreground">
            Scan this QR code with your authenticator app (like Google Authenticator, Authy, or 1Password).
          </p>

          <!-- QR Code -->
          <div v-if="setupData?.qrCodeDataUri" class="flex justify-center">
            <img
              :src="setupData.qrCodeDataUri"
              alt="TOTP QR Code"
              class="w-48 h-48 border rounded-lg"
            />
          </div>

          <!-- Manual Entry -->
          <div class="space-y-2">
            <Label class="text-sm text-muted-foreground">Or enter this code manually:</Label>
            <div class="flex items-center gap-2">
              <code class="flex-1 p-2 bg-muted rounded text-sm font-mono break-all">
                {{ setupData?.secret }}
              </code>
              <Button variant="outline" size="icon" @click="copySecret">
                <Check v-if="copiedSecret" class="h-4 w-4" />
                <Copy v-else class="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>

        <!-- Verification Code Input -->
        <div class="space-y-2">
          <Label for="verificationCode">Enter the 6-digit code from your app:</Label>
          <Input
            id="verificationCode"
            :value="verificationCode"
            type="text"
            inputmode="numeric"
            maxlength="6"
            placeholder="000000"
            class="text-center text-lg tracking-widest font-mono"
            @input="handleCodeInput"
          />
        </div>

        <DialogFooter>
          <Button variant="outline" @click="handleClose">Cancel</Button>
          <Button :disabled="verificationCode.length !== 6 || loading" @click="verifySetup">
            <Loader2 v-if="loading" class="mr-2 h-4 w-4 animate-spin" />
            Verify
          </Button>
        </DialogFooter>
      </div>

      <!-- Recovery Codes Step -->
      <div v-else-if="step === 'recovery'" class="py-4 space-y-6">
        <Alert class="bg-amber-50 border-amber-200 text-amber-800">
          <AlertDescription>
            Save these recovery codes in a safe place. You can use them to access your account if you lose your authenticator device.
            Each code can only be used once.
          </AlertDescription>
        </Alert>

        <div class="space-y-3">
          <div class="grid grid-cols-2 gap-2 p-4 bg-muted rounded-lg font-mono text-sm">
            <span v-for="code in recoveryCodes" :key="code" class="text-center py-1">
              {{ code }}
            </span>
          </div>

          <div class="flex gap-2">
            <Button variant="outline" class="flex-1" @click="copyCodes">
              <Check v-if="copiedCodes" class="mr-2 h-4 w-4" />
              <Copy v-else class="mr-2 h-4 w-4" />
              {{ copiedCodes ? 'Copied!' : 'Copy Codes' }}
            </Button>
            <Button variant="outline" class="flex-1" @click="downloadCodes">
              <Check v-if="downloadedCodes" class="mr-2 h-4 w-4" />
              <Download v-else class="mr-2 h-4 w-4" />
              {{ downloadedCodes ? 'Downloaded!' : 'Download' }}
            </Button>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <input
            id="savedCodes"
            v-model="savedCodesConfirmed"
            type="checkbox"
            :disabled="!canConfirmSaved"
            class="h-4 w-4 text-primary focus:ring-primary/20 border-input rounded transition-colors disabled:opacity-50"
          />
          <Label
            for="savedCodes"
            :class="{ 'text-muted-foreground': !canConfirmSaved }"
            class="font-normal cursor-pointer"
          >
            I have saved my recovery codes
          </Label>
        </div>

        <DialogFooter>
          <Button :disabled="!savedCodesConfirmed" @click="handleDone">
            Done
          </Button>
        </DialogFooter>
      </div>
    </DialogScrollContent>
  </Dialog>
</template>
