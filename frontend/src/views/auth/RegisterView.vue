<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Eye, EyeOff, Loader2, Check, Circle } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'

const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const error = ref('')

const passwordValidation = computed(() => ({
  minLength: password.value.length >= 8,
  hasUppercase: /[A-Z]/.test(password.value),
  hasNumber: /\d/.test(password.value),
}))

const isPasswordValid = computed(() => {
  return passwordValidation.value.minLength &&
         passwordValidation.value.hasUppercase &&
         passwordValidation.value.hasNumber
})

const passwordsMatch = computed(() => {
  return password.value === confirmPassword.value && confirmPassword.value !== ''
})

const isValid = computed(() => {
  return email.value.trim() !== '' &&
         isPasswordValid.value &&
         passwordsMatch.value
})

async function handleSubmit() {
  if (!isValid.value) return

  error.value = ''
  try {
    await authStore.register({
      email: email.value,
      password: password.value,
    })
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    error.value = axiosError.response?.data?.message || 'Registration failed. Please try again.'
  }
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
        <CardHeader>
          <CardTitle class="text-2xl">Create Your Account</CardTitle>
          <CardDescription>Enter your details to get started</CardDescription>
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
                  autocomplete="new-password"
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

            <div class="space-y-2">
              <Label for="confirm-password">Confirm Password</Label>
              <div class="relative">
                <Input
                  id="confirm-password"
                  v-model="confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  required
                  class="pr-10"
                  :class="{ 'border-destructive focus-visible:ring-destructive': confirmPassword && !passwordsMatch }"
                />
                <button
                  type="button"
                  class="absolute inset-y-0 right-0 flex items-center pr-3 text-muted-foreground hover:text-foreground transition-colors"
                  @click="showConfirmPassword = !showConfirmPassword"
                >
                  <EyeOff v-if="showConfirmPassword" class="h-5 w-5" />
                  <Eye v-else class="h-5 w-5" />
                </button>
              </div>
              <p v-if="confirmPassword && !passwordsMatch" class="text-sm text-destructive">
                Passwords do not match
              </p>
            </div>

            <div class="space-y-2 bg-muted/50 rounded-lg p-4">
              <p class="text-sm font-medium text-muted-foreground">Password Requirements:</p>
              <ul class="text-sm space-y-1.5">
                <li :class="['flex items-center transition-colors', passwordValidation.minLength ? 'text-green-600' : 'text-muted-foreground']">
                  <Check v-if="passwordValidation.minLength" class="mr-2 h-3 w-3" />
                  <Circle v-else class="mr-2 h-3 w-3" />
                  At least 8 characters
                </li>
                <li :class="['flex items-center transition-colors', passwordValidation.hasUppercase ? 'text-green-600' : 'text-muted-foreground']">
                  <Check v-if="passwordValidation.hasUppercase" class="mr-2 h-3 w-3" />
                  <Circle v-else class="mr-2 h-3 w-3" />
                  Contains uppercase letter
                </li>
                <li :class="['flex items-center transition-colors', passwordValidation.hasNumber ? 'text-green-600' : 'text-muted-foreground']">
                  <Check v-if="passwordValidation.hasNumber" class="mr-2 h-3 w-3" />
                  <Circle v-else class="mr-2 h-3 w-3" />
                  Contains number
                </li>
              </ul>
            </div>

            <Button
              type="submit"
              :disabled="!isValid || authStore.loading"
              class="w-full"
            >
              <Loader2 v-if="authStore.loading" class="mr-2 h-4 w-4 animate-spin" />
              {{ authStore.loading ? 'Creating Account...' : 'Create Account' }}
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
              Already have an account?
              <router-link to="/login" class="font-medium text-primary hover:text-primary/80 transition-colors">
                Login
              </router-link>
            </p>
          </div>
        </CardContent>
      </Card>

      <p class="mt-8 text-center text-sm text-muted-foreground">
        pennywise.cheap
      </p>
    </div>
  </div>
</template>
