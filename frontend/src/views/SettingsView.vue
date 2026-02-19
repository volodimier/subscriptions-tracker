<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import { useAuthStore } from '@/stores/auth'
import { CURRENCIES } from '@/utils/constants'
import { formatDateTime } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import TwoFaSection from '@/components/settings/TwoFaSection.vue'
import EmailVerificationSection from '@/components/settings/EmailVerificationSection.vue'

const settingsStore = useSettingsStore()
const authStore = useAuthStore()

const selectedCurrency = ref('')
const selectedTimeZone = ref('')
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
    selectedTimeZone.value = settingsStore.settings.userTimeZone
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

async function updateTimeZone() {
  try {
    await settingsStore.updateUserTimeZone(selectedTimeZone.value.trim())
    successMessage.value = 'Timezone updated successfully'
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

    <Alert v-if="successMessage" class="mb-6 bg-green-50 border-green-200 text-green-700">
      <AlertDescription>{{ successMessage }}</AlertDescription>
    </Alert>

    <div v-if="settingsStore.loading && !settingsStore.settings" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <template v-else-if="settingsStore.settings">
      <!-- User Profile -->
      <Card class="mb-6">
        <CardHeader>
          <CardTitle>User Profile</CardTitle>
        </CardHeader>
        <CardContent class="space-y-4">
          <div>
            <Label>Email</Label>
            <p class="text-gray-900 mt-1">{{ settingsStore.settings.email }}</p>
            <p class="text-sm text-muted-foreground">(Cannot be changed)</p>
          </div>
          <div>
            <Button variant="outline" @click="showPasswordDialog = true">
              Change Password
            </Button>
          </div>

          <!-- Email Verification Section -->
          <EmailVerificationSection v-if="authStore.user?.emailVerificationEnabled" />

          <!-- Two-Factor Authentication Section -->
          <TwoFaSection />
        </CardContent>
      </Card>

      <!-- Preferences -->
      <Card class="mb-6">
        <CardHeader>
          <CardTitle>Preferences</CardTitle>
        </CardHeader>
        <CardContent class="space-y-4">
          <div>
            <Label>Base Currency</Label>
            <div class="flex items-center space-x-3 mt-2">
              <div class="flex space-x-2">
                <label
                  v-for="currency in CURRENCIES"
                  :key="currency"
                  :class="[
                    'flex items-center justify-center px-4 py-2 border rounded-lg cursor-pointer transition-colors',
                    selectedCurrency === currency
                      ? 'border-primary bg-primary/5 text-primary'
                      : 'border-input hover:border-gray-400'
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
              <Button
                :disabled="selectedCurrency === settingsStore.settings.baseCurrency"
                @click="updateCurrency"
              >
                Save
              </Button>
            </div>
            <p class="text-sm text-muted-foreground mt-2">
              Changing base currency will affect how statistics are calculated
            </p>
          </div>
          <div>
            <Label for="userTimeZone">Billing Timezone (IANA)</Label>
            <div class="flex items-center space-x-3 mt-2">
              <Input
                id="userTimeZone"
                v-model="selectedTimeZone"
                type="text"
                placeholder="e.g., UTC or Europe/Warsaw"
                class="max-w-md"
              />
              <Button
                :disabled="selectedTimeZone.trim() === settingsStore.settings.userTimeZone"
                @click="updateTimeZone"
              >
                Save
              </Button>
            </div>
            <p class="text-sm text-muted-foreground mt-2">
              Timezone is used for recurrence cutoff and payment due processing.
            </p>
          </div>
        </CardContent>
      </Card>

      <!-- FX Rates -->
      <Card class="mb-6">
        <CardHeader>
          <CardTitle>FX Rates</CardTitle>
        </CardHeader>
        <CardContent class="space-y-4">
          <div>
            <p class="text-sm text-muted-foreground">
              Last updated: {{ settingsStore.settings.fxRatesLastUpdated ? formatDateTime(settingsStore.settings.fxRatesLastUpdated) : 'Never' }}
            </p>
            <Button v-if="authStore.isAdmin" variant="outline" class="mt-2" @click="handleRefreshFxRates">
              Refresh Rates Now
            </Button>
          </div>
          <div v-if="settingsStore.settings.currentFxRates">
            <p class="text-sm text-muted-foreground mb-2">Current rates (to {{ settingsStore.settings.baseCurrency }}):</p>
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
        </CardContent>
      </Card>

      <!-- Account Management -->
      <Card class="border-red-200">
        <CardHeader>
          <CardTitle class="text-red-600">Danger Zone</CardTitle>
        </CardHeader>
        <CardContent class="space-y-4">
          <div>
            <p class="text-sm text-muted-foreground mb-2">
              Permanently delete your account and all associated data. This action cannot be undone.
            </p>
            <Button variant="destructive" @click="showDeleteDialog = true">
              Delete Account
            </Button>
          </div>
        </CardContent>
      </Card>
    </template>

    <!-- Change Password Dialog -->
    <Dialog v-model:open="showPasswordDialog">
      <DialogContent class="sm:max-w-md" @escape-key-down="closePasswordDialog" @pointer-down-outside="closePasswordDialog">
        <DialogHeader>
          <DialogTitle>Change Password</DialogTitle>
        </DialogHeader>

        <Alert v-if="passwordError" variant="destructive" class="mt-2">
          <AlertDescription>{{ passwordError }}</AlertDescription>
        </Alert>

        <form class="space-y-4 py-4" @submit.prevent="handleChangePassword">
          <div>
            <Label for="currentPassword">Current Password</Label>
            <Input
              id="currentPassword"
              v-model="currentPassword"
              type="password"
              required
              class="mt-2"
            />
          </div>
          <div>
            <Label for="newPassword">New Password</Label>
            <Input
              id="newPassword"
              v-model="newPassword"
              type="password"
              required
              class="mt-2"
            />
          </div>
          <div>
            <Label for="confirmNewPassword">Confirm New Password</Label>
            <Input
              id="confirmNewPassword"
              v-model="confirmNewPassword"
              type="password"
              required
              class="mt-2"
            />
          </div>
          <DialogFooter class="pt-4">
            <Button type="button" variant="outline" @click="closePasswordDialog">Cancel</Button>
            <Button type="submit">Change Password</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>

    <!-- Delete Account Dialog -->
    <Dialog v-model:open="showDeleteDialog">
      <DialogContent class="sm:max-w-md" @escape-key-down="closeDeleteDialog" @pointer-down-outside="closeDeleteDialog">
        <DialogHeader>
          <DialogTitle class="text-red-600">Delete Account</DialogTitle>
          <DialogDescription>
            This will permanently delete your account, all subscriptions, services, and payment history.
            This action cannot be undone.
          </DialogDescription>
        </DialogHeader>

        <Alert v-if="deleteError" variant="destructive" class="mt-2">
          <AlertDescription>{{ deleteError }}</AlertDescription>
        </Alert>

        <form class="space-y-4 py-4" @submit.prevent="handleDeleteAccount">
          <div>
            <Label for="deletePassword">Password</Label>
            <Input
              id="deletePassword"
              v-model="deletePassword"
              type="password"
              required
              class="mt-2"
            />
          </div>
          <div>
            <Label for="deleteConfirmation">Type DELETE to confirm</Label>
            <Input
              id="deleteConfirmation"
              v-model="deleteConfirmation"
              type="text"
              required
              placeholder="DELETE"
              class="mt-2"
            />
          </div>
          <DialogFooter class="pt-4">
            <Button type="button" variant="outline" @click="closeDeleteDialog">Cancel</Button>
            <Button type="submit" variant="destructive">Delete My Account</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  </div>
</template>
