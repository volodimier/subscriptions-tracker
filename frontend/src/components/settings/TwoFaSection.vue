<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Shield, ShieldCheck, ShieldOff, RefreshCw, Loader2, Copy, Download, Check } from 'lucide-vue-next'
import { twoFaService } from '@/services/twoFaService'
import { formatDateTime } from '@/utils/formatters'
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
import TwoFaSetupDialog from './TwoFaSetupDialog.vue'
import TwoFaDisableDialog from './TwoFaDisableDialog.vue'
import type { TotpStatusResponse } from '@/types'

const status = ref<TotpStatusResponse | null>(null)
const loading = ref(false)
const error = ref('')

const showSetupDialog = ref(false)
const showDisableDialog = ref(false)
const showRegenerateDialog = ref(false)

// Regenerate dialog state
const regenerateCode = ref('')
const regenerateLoading = ref(false)
const regenerateError = ref('')
const newRecoveryCodes = ref<string[]>([])
const showNewCodes = ref(false)
const copiedCodes = ref(false)
const downloadedCodes = ref(false)
const savedCodesConfirmed = ref(false)

onMounted(() => {
  fetchStatus()
})

async function fetchStatus() {
  loading.value = true
  error.value = ''
  try {
    status.value = await twoFaService.getStatus()
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    error.value = axiosError.response?.data?.message || 'Failed to fetch 2FA status'
  } finally {
    loading.value = false
  }
}

function handleSetupSuccess() {
  fetchStatus()
}

function handleDisableSuccess() {
  fetchStatus()
}

async function handleRegenerate() {
  if (regenerateCode.value.length !== 6) {
    regenerateError.value = 'Please enter a 6-digit code'
    return
  }

  regenerateLoading.value = true
  regenerateError.value = ''
  try {
    const response = await twoFaService.regenerateRecoveryCodes(regenerateCode.value)
    newRecoveryCodes.value = response.recoveryCodes
    showNewCodes.value = true
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    regenerateError.value = axiosError.response?.data?.message || 'Failed to regenerate recovery codes'
  } finally {
    regenerateLoading.value = false
  }
}

async function copyCodes() {
  const content = newRecoveryCodes.value.join('\n')
  await navigator.clipboard.writeText(content)
  copiedCodes.value = true
}

function downloadCodes() {
  const content = newRecoveryCodes.value.join('\n')
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'recovery-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
  downloadedCodes.value = true
}

function closeRegenerateDialog() {
  showRegenerateDialog.value = false
  regenerateCode.value = ''
  regenerateError.value = ''
  regenerateLoading.value = false
  newRecoveryCodes.value = []
  showNewCodes.value = false
  copiedCodes.value = false
  downloadedCodes.value = false
  savedCodesConfirmed.value = false

  // Refresh status to get updated remaining codes count
  if (showNewCodes.value) {
    fetchStatus()
  }
}

function handleRegenerateCodeInput(event: Event) {
  const input = event.target as HTMLInputElement
  input.value = input.value.replace(/\D/g, '').slice(0, 6)
  regenerateCode.value = input.value
}
</script>

<template>
  <div class="space-y-4 pt-4 border-t">
    <div class="flex items-center gap-2">
      <Shield class="h-5 w-5 text-muted-foreground" />
      <h3 class="font-medium">Two-Factor Authentication</h3>
    </div>

    <Alert v-if="error" variant="destructive">
      <AlertDescription>{{ error }}</AlertDescription>
    </Alert>

    <div v-if="loading" class="flex items-center gap-2 py-2">
      <Loader2 class="h-4 w-4 animate-spin" />
      <span class="text-sm text-muted-foreground">Loading 2FA status...</span>
    </div>

    <template v-else-if="status">
      <!-- 2FA Disabled -->
      <div v-if="!status.enabled" class="space-y-3">
        <div class="flex items-center gap-2 text-muted-foreground">
          <ShieldOff class="h-4 w-4" />
          <span class="text-sm">Two-factor authentication is not enabled</span>
        </div>
        <p class="text-sm text-muted-foreground">
          Add an extra layer of security to your account by requiring a code from your authenticator app in addition to your password.
        </p>
        <Button variant="outline" @click="showSetupDialog = true">
          <Shield class="mr-2 h-4 w-4" />
          Enable 2FA
        </Button>
      </div>

      <!-- 2FA Enabled -->
      <div v-else class="space-y-3">
        <div class="flex items-center gap-2 text-green-600">
          <ShieldCheck class="h-4 w-4" />
          <span class="text-sm font-medium">Two-factor authentication is enabled</span>
        </div>

        <div class="text-sm text-muted-foreground space-y-1">
          <p v-if="status.enabledAt">
            Enabled: {{ formatDateTime(status.enabledAt) }}
          </p>
          <p v-if="status.remainingRecoveryCodes !== null">
            Recovery codes remaining: {{ status.remainingRecoveryCodes }}
          </p>
        </div>

        <div class="flex flex-wrap gap-2">
          <Button variant="outline" @click="showRegenerateDialog = true">
            <RefreshCw class="mr-2 h-4 w-4" />
            Regenerate Recovery Codes
          </Button>
          <Button variant="outline" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="showDisableDialog = true">
            <ShieldOff class="mr-2 h-4 w-4" />
            Disable 2FA
          </Button>
        </div>
      </div>
    </template>

    <!-- Setup Dialog -->
    <TwoFaSetupDialog
      v-model:open="showSetupDialog"
      @success="handleSetupSuccess"
    />

    <!-- Disable Dialog -->
    <TwoFaDisableDialog
      v-model:open="showDisableDialog"
      @success="handleDisableSuccess"
    />

    <!-- Regenerate Recovery Codes Dialog -->
    <Dialog :open="showRegenerateDialog" @update:open="(v) => { if (!v) closeRegenerateDialog() }">
      <DialogScrollContent class="sm:max-w-md" @escape-key-down.prevent @pointer-down-outside.prevent>
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2">
            <RefreshCw class="h-5 w-5" />
            Regenerate Recovery Codes
          </DialogTitle>
        </DialogHeader>

        <!-- Code entry step -->
        <template v-if="!showNewCodes">
          <Alert v-if="regenerateError" variant="destructive" class="mt-2">
            <AlertDescription>{{ regenerateError }}</AlertDescription>
          </Alert>

          <div class="py-4 space-y-4">
            <Alert class="bg-amber-50 border-amber-200 text-amber-800">
              <AlertDescription>
                This will invalidate all your existing recovery codes. Make sure to save the new codes.
              </AlertDescription>
            </Alert>

            <div class="space-y-2">
              <Label for="regenerateCode">Enter your authentication code:</Label>
              <Input
                id="regenerateCode"
                :value="regenerateCode"
                type="text"
                inputmode="numeric"
                maxlength="6"
                placeholder="000000"
                class="text-center text-lg tracking-widest font-mono"
                @input="handleRegenerateCodeInput"
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" @click="closeRegenerateDialog">Cancel</Button>
            <Button :disabled="regenerateCode.length !== 6 || regenerateLoading" @click="handleRegenerate">
              <Loader2 v-if="regenerateLoading" class="mr-2 h-4 w-4 animate-spin" />
              Regenerate
            </Button>
          </DialogFooter>
        </template>

        <!-- New codes display step -->
        <template v-else>
          <div class="py-4 space-y-6">
            <Alert class="bg-amber-50 border-amber-200 text-amber-800">
              <AlertDescription>
                Save these new recovery codes in a safe place. Your old codes are now invalid.
              </AlertDescription>
            </Alert>

            <div class="space-y-3">
              <div class="grid grid-cols-2 gap-2 p-4 bg-muted rounded-lg font-mono text-sm">
                <span v-for="code in newRecoveryCodes" :key="code" class="text-center py-1">
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
                id="savedNewCodes"
                v-model="savedCodesConfirmed"
                type="checkbox"
                :disabled="!copiedCodes && !downloadedCodes"
                class="h-4 w-4 text-primary focus:ring-primary/20 border-input rounded transition-colors disabled:opacity-50"
              />
              <Label
                for="savedNewCodes"
                :class="{ 'text-muted-foreground': !copiedCodes && !downloadedCodes }"
                class="font-normal cursor-pointer"
              >
                I have saved my recovery codes
              </Label>
            </div>
          </div>

          <DialogFooter>
            <Button :disabled="!savedCodesConfirmed" @click="closeRegenerateDialog">
              Done
            </Button>
          </DialogFooter>
        </template>
      </DialogScrollContent>
    </Dialog>
  </div>
</template>
