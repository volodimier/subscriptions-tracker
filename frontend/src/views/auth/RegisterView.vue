<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

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
        <h1 class="text-3xl font-semibold text-primary-600 tracking-tight">SubscriptionTracker</h1>
        <p class="mt-2 text-gray-500">Track your subscriptions simply</p>
      </div>

      <div class="bg-white rounded-2xl shadow-soft p-8 border border-gray-100">
        <h2 class="text-2xl font-semibold text-gray-900 mb-6 tracking-tight">Create Your Account</h2>

        <form class="space-y-6" @submit.prevent="handleSubmit">
          <div v-if="error" class="bg-red-50 border border-red-100 text-red-700 px-4 py-3 rounded-xl text-sm">
            {{ error }}
          </div>

          <div>
            <label for="email" class="label">Email Address</label>
            <input
              id="email"
              v-model="email"
              type="email"
              autocomplete="email"
              required
              class="input"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label for="password" class="label">Password</label>
            <div class="relative">
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                required
                class="input pr-10"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 transition-colors"
                @click="showPassword = !showPassword"
              >
                <svg v-if="!showPassword" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
          </div>

          <div>
            <label for="confirm-password" class="label">Confirm Password</label>
            <div class="relative">
              <input
                id="confirm-password"
                v-model="confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                autocomplete="new-password"
                required
                class="input pr-10"
                :class="{ 'input-error': confirmPassword && !passwordsMatch }"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 transition-colors"
                @click="showConfirmPassword = !showConfirmPassword"
              >
                <svg v-if="!showConfirmPassword" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
            <p v-if="confirmPassword && !passwordsMatch" class="mt-1 text-sm text-red-600">
              Passwords do not match
            </p>
          </div>

          <div class="space-y-2 bg-gray-50/50 rounded-xl p-4">
            <p class="text-sm font-medium text-gray-600">Password Requirements:</p>
            <ul class="text-sm space-y-1.5">
              <li :class="['flex items-center transition-colors', passwordValidation.minLength ? 'text-green-600' : 'text-gray-400']">
                <span class="mr-2 text-xs">{{ passwordValidation.minLength ? '\u2713' : '\u25CB' }}</span>
                At least 8 characters
              </li>
              <li :class="['flex items-center transition-colors', passwordValidation.hasUppercase ? 'text-green-600' : 'text-gray-400']">
                <span class="mr-2 text-xs">{{ passwordValidation.hasUppercase ? '\u2713' : '\u25CB' }}</span>
                Contains uppercase letter
              </li>
              <li :class="['flex items-center transition-colors', passwordValidation.hasNumber ? 'text-green-600' : 'text-gray-400']">
                <span class="mr-2 text-xs">{{ passwordValidation.hasNumber ? '\u2713' : '\u25CB' }}</span>
                Contains number
              </li>
            </ul>
          </div>

          <button
            type="submit"
            :disabled="!isValid || authStore.loading"
            class="w-full btn-primary py-3 flex items-center justify-center"
          >
            <LoadingSpinner v-if="authStore.loading" size="sm" class="mr-2" />
            {{ authStore.loading ? 'Creating Account...' : 'Create Account' }}
          </button>
        </form>

        <div class="mt-6">
          <div class="relative">
            <div class="absolute inset-0 flex items-center">
              <div class="w-full border-t border-gray-200" />
            </div>
            <div class="relative flex justify-center text-sm">
              <span class="px-3 bg-white text-gray-400">or</span>
            </div>
          </div>

          <p class="mt-6 text-center text-sm text-gray-500">
            Already have an account?
            <router-link to="/login" class="font-medium text-primary-600 hover:text-primary-500 transition-colors">
              Login
            </router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
