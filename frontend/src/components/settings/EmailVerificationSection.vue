<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Mail, CheckCircle, AlertTriangle, Loader2, Clock } from 'lucide-vue-next'
import { emailVerificationService } from '@/services/emailVerificationService'
import { formatDateTime } from '@/utils/formatters'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import type { EmailVerificationStatus } from '@/types'

const status = ref<EmailVerificationStatus | null>(null)
const loading = ref(false)
const resending = ref(false)
const error = ref('')
const successMessage = ref('')
const resendCountdown = ref(0)

let countdownInterval: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchStatus()
})

onUnmounted(() => {
  if (countdownInterval) {
    clearInterval(countdownInterval)
  }
})

async function fetchStatus() {
  loading.value = true
  error.value = ''
  try {
    status.value = await emailVerificationService.getStatus()
    startCountdownIfNeeded()
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    error.value = axiosError.response?.data?.message || 'Failed to fetch email verification status'
  } finally {
    loading.value = false
  }
}

function startCountdownIfNeeded() {
  if (countdownInterval) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }

  if (status.value && !status.value.canResend && status.value.resendAvailableInSeconds) {
    resendCountdown.value = status.value.resendAvailableInSeconds
    countdownInterval = setInterval(() => {
      resendCountdown.value -= 1
      if (resendCountdown.value <= 0) {
        if (countdownInterval) {
          clearInterval(countdownInterval)
          countdownInterval = null
        }
        // Refresh status to get updated canResend value
        fetchStatus()
      }
    }, 1000)
  }
}

async function handleResend() {
  resending.value = true
  error.value = ''
  successMessage.value = ''
  try {
    await emailVerificationService.resendVerificationEmail()
    successMessage.value = 'Verification email sent! Please check your inbox.'
    // Refresh status to get updated resend availability
    await fetchStatus()
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    error.value = axiosError.response?.data?.message || 'Failed to resend verification email'
  } finally {
    resending.value = false
  }
}

const formattedGracePeriodEnd = computed(() => {
  if (status.value?.gracePeriodEndsAt) {
    return formatDateTime(status.value.gracePeriodEndsAt)
  }
  return null
})

const canResend = computed(() => {
  return status.value?.canResend && resendCountdown.value <= 0
})

const resendButtonText = computed(() => {
  if (resending.value) return 'Sending...'
  if (resendCountdown.value > 0) return `Resend available in ${resendCountdown.value}s`
  return 'Resend Verification Email'
})
</script>

<template>
  <div class="space-y-4 pt-4 border-t">
    <div class="flex items-center gap-2">
      <Mail class="h-5 w-5 text-muted-foreground" />
      <h3 class="font-medium">Email Verification</h3>
    </div>

    <Alert v-if="error" variant="destructive">
      <AlertDescription>{{ error }}</AlertDescription>
    </Alert>

    <Alert v-if="successMessage" class="bg-green-50 border-green-200 text-green-700">
      <AlertDescription>{{ successMessage }}</AlertDescription>
    </Alert>

    <div v-if="loading" class="flex items-center gap-2 py-2">
      <Loader2 class="h-4 w-4 animate-spin" />
      <span class="text-sm text-muted-foreground">Loading email verification status...</span>
    </div>

    <template v-else-if="status">
      <!-- Email Verified -->
      <div v-if="status.verified" class="space-y-3">
        <div class="flex items-center gap-2 text-green-600">
          <CheckCircle class="h-4 w-4" />
          <span class="text-sm font-medium">Email verified</span>
          <Badge variant="outline" class="border-green-600 text-green-600">Verified</Badge>
        </div>
        <p v-if="status.verifiedAt" class="text-sm text-muted-foreground">
          Verified on {{ formatDateTime(status.verifiedAt) }}
        </p>
      </div>

      <!-- Email Not Verified -->
      <div v-else class="space-y-3">
        <div class="flex items-center gap-2 text-amber-600">
          <AlertTriangle class="h-4 w-4" />
          <span class="text-sm font-medium">Email not verified</span>
          <Badge variant="outline" class="border-amber-600 text-amber-600">Unverified</Badge>
        </div>

        <p class="text-sm text-muted-foreground">
          Please verify your email address to ensure full access to your account.
        </p>

        <!-- Grace Period Warning -->
        <Alert v-if="status.withinGracePeriod && formattedGracePeriodEnd" class="bg-amber-50 border-amber-200 text-amber-800">
          <AlertTriangle class="h-4 w-4" />
          <AlertDescription class="ml-2">
            <div class="flex items-center gap-2">
              <Clock class="h-4 w-4" />
              <span>
                <strong>Warning:</strong> Your account will be deleted on {{ formattedGracePeriodEnd }} if email is not verified.
              </span>
            </div>
          </AlertDescription>
        </Alert>

        <!-- Resend Button -->
        <Button
          variant="outline"
          :disabled="!canResend || resending"
          @click="handleResend"
        >
          <Loader2 v-if="resending" class="mr-2 h-4 w-4 animate-spin" />
          <Mail v-else class="mr-2 h-4 w-4" />
          {{ resendButtonText }}
        </Button>
      </div>
    </template>
  </div>
</template>
