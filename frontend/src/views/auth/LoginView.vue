<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Eye, EyeOff, Loader2, Shield, ArrowLeft } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'

const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const rememberMe = ref(false)
const showPassword = ref(false)
const error = ref('')

// 2FA state
const showTwoFactorStep = ref(false)
const totpCode = ref('')
const showRecoveryInput = ref(false)
const recoveryCode = ref('')
const totpError = ref('')

const isValid = computed(() => {
  return email.value.trim() !== '' && password.value.trim() !== ''
})

const isTotpValid = computed(() => {
  return totpCode.value.length === 6
})

const isRecoveryValid = computed(() => {
  return recoveryCode.value.trim().length > 0
})

async function handleSubmit() {
  if (!isValid.value) return

  error.value = ''
  try {
    const response = await authStore.login({
      email: email.value,
      password: password.value,
      rememberMe: rememberMe.value,
    })

    // Check if 2FA is required
    if (response.twoFactorRequired) {
      showTwoFactorStep.value = true
    }
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    error.value = axiosError.response?.data?.message || 'Login failed. Please try again.'
  }
}

async function handleTotpSubmit() {
  if (!isTotpValid.value) return

  totpError.value = ''
  try {
    await authStore.verifyTotp(totpCode.value)
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    totpError.value = axiosError.response?.data?.message || 'Invalid code. Please try again.'
  }
}

async function handleRecoverySubmit() {
  if (!isRecoveryValid.value) return

  totpError.value = ''
  try {
    await authStore.verifyRecoveryCode(recoveryCode.value)
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    totpError.value = axiosError.response?.data?.message || 'Invalid recovery code. Please try again.'
  }
}

function handleTotpCodeInput(event: Event) {
  const input = event.target as HTMLInputElement
  input.value = input.value.replace(/\D/g, '').slice(0, 6)
  totpCode.value = input.value
}

function backToLogin() {
  showTwoFactorStep.value = false
  showRecoveryInput.value = false
  totpCode.value = ''
  recoveryCode.value = ''
  totpError.value = ''
  authStore.clearPartialAuth()
}

function toggleRecoveryMode() {
  showRecoveryInput.value = !showRecoveryInput.value
  totpCode.value = ''
  recoveryCode.value = ''
  totpError.value = ''
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100/80 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div class="text-center">
        <h1 class="text-3xl font-semibold text-foreground tracking-tight">PennyWise</h1>
        <p class="mt-2 text-muted-foreground">Be wise with your money</p>
      </div>

      <Card>
        <!-- Login Form -->
        <template v-if="!showTwoFactorStep">
          <CardHeader>
            <CardTitle class="text-2xl">Login to Your Account</CardTitle>
            <CardDescription>Enter your credentials to access your account</CardDescription>
          </CardHeader>
          <CardContent>
            <form class="space-y-6" @submit.prevent="handleSubmit">
              <Alert v-if="error" variant="destructive">
                <AlertDescription>{{ error }}</AlertDescription>
              </Alert>

              <div class="space-y-2">
                <Label for="email">Email Address</Label>
                <Input
                  id="email"
                  v-model="email"
                  type="email"
                  autocomplete="email"
                  required
                  placeholder="you@example.com"
                />
              </div>

              <div class="space-y-2">
                <Label for="password">Password</Label>
                <div class="relative">
                  <Input
                    id="password"
                    v-model="password"
                    :type="showPassword ? 'text' : 'password'"
                    autocomplete="current-password"
                    required
                    class="pr-10"
                  />
                  <button
                    type="button"
                    class="absolute inset-y-0 right-0 flex items-center pr-3 text-muted-foreground hover:text-foreground transition-colors"
                    @click="showPassword = !showPassword"
                  >
                    <EyeOff v-if="showPassword" class="h-5 w-5" />
                    <Eye v-else class="h-5 w-5" />
                  </button>
                </div>
              </div>

              <div class="flex items-center">
                <input
                  id="remember-me"
                  v-model="rememberMe"
                  type="checkbox"
                  class="h-4 w-4 text-primary focus:ring-primary/20 border-input rounded transition-colors"
                />
                <Label for="remember-me" class="ml-2 font-normal">
                  Remember me
                </Label>
              </div>

              <Button
                type="submit"
                :disabled="!isValid || authStore.loading"
                class="w-full"
              >
                <Loader2 v-if="authStore.loading" class="mr-2 h-4 w-4 animate-spin" />
                {{ authStore.loading ? 'Logging in...' : 'Login' }}
              </Button>
            </form>

            <div class="mt-6">
              <div class="relative">
                <div class="absolute inset-0 flex items-center">
                  <div class="w-full border-t border-border" />
                </div>
                <div class="relative flex justify-center text-sm">
                  <span class="px-3 bg-card text-muted-foreground">or</span>
                </div>
              </div>

              <p class="mt-6 text-center text-sm text-muted-foreground">
                Don't have an account?
                <router-link to="/register" class="font-medium text-primary hover:text-primary/80 transition-colors">
                  Sign Up
                </router-link>
              </p>
            </div>
          </CardContent>
        </template>

        <!-- Two-Factor Authentication Step -->
        <template v-else>
          <CardHeader>
            <div class="flex items-center gap-2">
              <button
                type="button"
                class="p-1 rounded-md hover:bg-muted transition-colors"
                @click="backToLogin"
              >
                <ArrowLeft class="h-5 w-5" />
              </button>
              <div>
                <CardTitle class="text-2xl flex items-center gap-2">
                  <Shield class="h-6 w-6" />
                  Two-Factor Authentication
                </CardTitle>
                <CardDescription>
                  {{ showRecoveryInput ? 'Enter a recovery code to access your account' : 'Enter the code from your authenticator app' }}
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Alert v-if="totpError" variant="destructive" class="mb-6">
              <AlertDescription>{{ totpError }}</AlertDescription>
            </Alert>

            <!-- TOTP Code Input -->
            <form v-if="!showRecoveryInput" class="space-y-6" @submit.prevent="handleTotpSubmit">
              <div class="space-y-2">
                <Label for="totpCode">Authentication Code</Label>
                <Input
                  id="totpCode"
                  :value="totpCode"
                  type="text"
                  inputmode="numeric"
                  maxlength="6"
                  placeholder="000000"
                  class="text-center text-2xl tracking-[0.5em] font-mono"
                  autocomplete="one-time-code"
                  @input="handleTotpCodeInput"
                />
                <p class="text-xs text-muted-foreground text-center">
                  Enter the 6-digit code from your authenticator app
                </p>
              </div>

              <Button
                type="submit"
                :disabled="!isTotpValid || authStore.loading"
                class="w-full"
              >
                <Loader2 v-if="authStore.loading" class="mr-2 h-4 w-4 animate-spin" />
                {{ authStore.loading ? 'Verifying...' : 'Verify' }}
              </Button>
            </form>

            <!-- Recovery Code Input -->
            <form v-else class="space-y-6" @submit.prevent="handleRecoverySubmit">
              <div class="space-y-2">
                <Label for="recoveryCode">Recovery Code</Label>
                <Input
                  id="recoveryCode"
                  v-model="recoveryCode"
                  type="text"
                  placeholder="Enter your recovery code"
                  class="text-center font-mono"
                  autocomplete="off"
                />
                <p class="text-xs text-muted-foreground text-center">
                  Recovery codes can only be used once
                </p>
              </div>

              <Button
                type="submit"
                :disabled="!isRecoveryValid || authStore.loading"
                class="w-full"
              >
                <Loader2 v-if="authStore.loading" class="mr-2 h-4 w-4 animate-spin" />
                {{ authStore.loading ? 'Verifying...' : 'Verify Recovery Code' }}
              </Button>
            </form>

            <div class="mt-6 text-center">
              <button
                type="button"
                class="text-sm text-primary hover:text-primary/80 transition-colors"
                @click="toggleRecoveryMode"
              >
                {{ showRecoveryInput ? 'Use authenticator app instead' : 'Use a recovery code' }}
              </button>
            </div>
          </CardContent>
        </template>
      </Card>
    </div>
  </div>
</template>
