<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import { useAuthStore } from '@/stores/auth'
import { CURRENCIES } from '@/utils/constants'
import { formatDateTime } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const settingsStore = useSettingsStore()
const authStore = useAuthStore()

const selectedCurrency = ref('')
const showPasswordDialog = ref(false)
const showDeleteDialog = ref(false)
const currentPassword = ref('')
const newPassword = ref('')
const confirmNewPassword = ref('')
const deletePassword = ref('')
const deleteConfirmation = ref('')
const passwordError = ref('')
const deleteError = ref('')
const successMessage = ref('')

onMounted(async () => {
  await settingsStore.fetchSettings()
  if (settingsStore.settings) {
    selectedCurrency.value = settingsStore.settings.baseCurrency
  }
})

async function updateCurrency() {
  try {
    await settingsStore.updateBaseCurrency(selectedCurrency.value)
    successMessage.value = 'Base currency updated successfully'
    setTimeout(() => successMessage.value = '', 3000)
  } catch {
    // Error handled in store
  }
}

async function handleChangePassword() {
  passwordError.value = ''

  if (newPassword.value !== confirmNewPassword.value) {
    passwordError.value = 'Passwords do not match'
    return
  }

  try {
    await settingsStore.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    })
    showPasswordDialog.value = false
    currentPassword.value = ''
    newPassword.value = ''
    confirmNewPassword.value = ''
    successMessage.value = 'Password changed successfully'
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    passwordError.value = axiosError.response?.data?.message || 'Failed to change password'
  }
}

async function handleDeleteAccount() {
  deleteError.value = ''

  if (deleteConfirmation.value !== 'DELETE') {
    deleteError.value = 'Please type DELETE to confirm'
    return
  }

  try {
    await settingsStore.deleteAccount({
      password: deletePassword.value,
      confirmation: deleteConfirmation.value,
    })
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    deleteError.value = axiosError.response?.data?.message || 'Failed to delete account'
  }
}

async function handleRefreshFxRates() {
  try {
    await settingsStore.refreshFxRates()
    successMessage.value = 'FX rates refreshed successfully'
    setTimeout(() => successMessage.value = '', 3000)
  } catch {
    // Error handled in store
  }
}

function closePasswordDialog() {
  showPasswordDialog.value = false
  currentPassword.value = ''
  newPassword.value = ''
  confirmNewPassword.value = ''
  passwordError.value = ''
}

function closeDeleteDialog() {
  showDeleteDialog.value = false
  deletePassword.value = ''
  deleteConfirmation.value = ''
  deleteError.value = ''
}
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Settings</h1>

    <div v-if="successMessage" class="mb-6 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg">
      {{ successMessage }}
    </div>

    <div v-if="settingsStore.loading && !settingsStore.settings" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <template v-else-if="settingsStore.settings">
      <!-- User Profile -->
      <div class="card mb-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">User Profile</h2>
        <div class="space-y-4">
          <div>
            <label class="label">Email</label>
            <p class="text-gray-900">{{ settingsStore.settings.email }}</p>
            <p class="text-sm text-gray-500">(Cannot be changed)</p>
          </div>
          <div>
            <button class="btn-secondary" @click="showPasswordDialog = true">
              Change Password
            </button>
          </div>
        </div>
      </div>

      <!-- Preferences -->
      <div class="card mb-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Preferences</h2>
        <div class="space-y-4">
          <div>
            <label class="label">Base Currency</label>
            <div class="flex items-center space-x-3">
              <div class="flex space-x-2">
                <label
                  v-for="currency in CURRENCIES"
                  :key="currency"
                  :class="[
                    'flex items-center justify-center px-4 py-2 border rounded-lg cursor-pointer transition-colors',
                    selectedCurrency === currency
                      ? 'border-primary-500 bg-primary-50 text-primary-700'
                      : 'border-gray-300 hover:border-gray-400'
                  ]"
                >
                  <input
                    v-model="selectedCurrency"
                    type="radio"
                    :value="currency"
                    class="sr-only"
                  />
                  <span class="font-medium">{{ currency }}</span>
                </label>
              </div>
              <button
                :disabled="selectedCurrency === settingsStore.settings.baseCurrency"
                class="btn-primary"
                @click="updateCurrency"
              >
                Save
              </button>
            </div>
            <p class="text-sm text-gray-500 mt-2">
              Changing base currency will affect how statistics are calculated
            </p>
          </div>
        </div>
      </div>

      <!-- FX Rates -->
      <div class="card mb-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">FX Rates</h2>
        <div class="space-y-4">
          <div>
            <p class="text-sm text-gray-500">
              Last updated: {{ settingsStore.settings.fxRatesLastUpdated ? formatDateTime(settingsStore.settings.fxRatesLastUpdated) : 'Never' }}
            </p>
            <button v-if="authStore.isAdmin" class="btn-secondary mt-2" @click="handleRefreshFxRates">
              Refresh Rates Now
            </button>
          </div>
          <div v-if="settingsStore.settings.currentFxRates">
            <p class="text-sm text-gray-500 mb-2">Current rates (to {{ settingsStore.settings.baseCurrency }}):</p>
            <div class="flex space-x-4 flex-wrap gap-y-1">
              <span
                v-for="(rate, currency) in settingsStore.settings.currentFxRates"
                :key="currency"
                class="text-sm"
              >
                <span class="font-medium">{{ currency }}:</span> {{ rate.toFixed(4) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Account Management -->
      <div class="card border-red-200">
        <h2 class="text-lg font-semibold text-red-600 mb-4">Danger Zone</h2>
        <div class="space-y-4">
          <div>
            <p class="text-sm text-gray-600 mb-2">
              Permanently delete your account and all associated data. This action cannot be undone.
            </p>
            <button class="btn-danger" @click="showDeleteDialog = true">
              Delete Account
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Change Password Dialog -->
    <div v-if="showPasswordDialog" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex min-h-full items-center justify-center p-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="closePasswordDialog" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h2 class="text-xl font-semibold mb-4">Change Password</h2>

          <div v-if="passwordError" class="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
            {{ passwordError }}
          </div>

          <form class="space-y-4" @submit.prevent="handleChangePassword">
            <div>
              <label for="currentPassword" class="label">Current Password</label>
              <input
                id="currentPassword"
                v-model="currentPassword"
                type="password"
                required
                class="input"
              />
            </div>
            <div>
              <label for="newPassword" class="label">New Password</label>
              <input
                id="newPassword"
                v-model="newPassword"
                type="password"
                required
                class="input"
              />
            </div>
            <div>
              <label for="confirmNewPassword" class="label">Confirm New Password</label>
              <input
                id="confirmNewPassword"
                v-model="confirmNewPassword"
                type="password"
                required
                class="input"
              />
            </div>
            <div class="flex justify-end space-x-3 pt-4">
              <button type="button" class="btn-secondary" @click="closePasswordDialog">Cancel</button>
              <button type="submit" class="btn-primary">Change Password</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Delete Account Dialog -->
    <div v-if="showDeleteDialog" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex min-h-full items-center justify-center p-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="closeDeleteDialog" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h2 class="text-xl font-semibold text-red-600 mb-4">Delete Account</h2>

          <p class="text-gray-600 mb-4">
            This will permanently delete your account, all subscriptions, services, and payment history.
            This action cannot be undone.
          </p>

          <div v-if="deleteError" class="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
            {{ deleteError }}
          </div>

          <form class="space-y-4" @submit.prevent="handleDeleteAccount">
            <div>
              <label for="deletePassword" class="label">Password</label>
              <input
                id="deletePassword"
                v-model="deletePassword"
                type="password"
                required
                class="input"
              />
            </div>
            <div>
              <label for="deleteConfirmation" class="label">Type DELETE to confirm</label>
              <input
                id="deleteConfirmation"
                v-model="deleteConfirmation"
                type="text"
                required
                class="input"
                placeholder="DELETE"
              />
            </div>
            <div class="flex justify-end space-x-3 pt-4">
              <button type="button" class="btn-secondary" @click="closeDeleteDialog">Cancel</button>
              <button type="submit" class="btn-danger">Delete My Account</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
