<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CheckCircle, XCircle, Loader2, Mail } from 'lucide-vue-next'
import { emailVerificationService } from '@/services/emailVerificationService'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const success = ref(false)
const error = ref('')

onMounted(async () => {
  const token = route.query.token as string

  if (!token) {
    error.value = 'No verification token provided'
    loading.value = false
    return
  }

  try {
    await emailVerificationService.verifyEmail(token)
    success.value = true
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string; code?: string } } }
    const errorCode = axiosError.response?.data?.code

    if (errorCode === 'TOKEN_EXPIRED') {
      error.value = 'This verification link has expired. Please request a new one.'
    } else if (errorCode === 'TOKEN_INVALID') {
      error.value = 'This verification link is invalid. Please request a new one.'
    } else if (errorCode === 'ALREADY_VERIFIED') {
      error.value = 'Your email has already been verified.'
    } else {
      error.value = axiosError.response?.data?.message || 'Failed to verify email. Please try again.'
    }
  } finally {
    loading.value = false
  }
})

function navigateToLogin() {
  router.push('/login')
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
        <!-- Loading State -->
        <template v-if="loading">
          <CardHeader>
            <CardTitle class="text-2xl flex items-center gap-2">
              <Mail class="h-6 w-6" />
              Email Verification
            </CardTitle>
            <CardDescription>Verifying your email address...</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="flex flex-col items-center justify-center py-8">
              <Loader2 class="h-12 w-12 animate-spin text-primary" />
              <p class="mt-4 text-muted-foreground">Please wait while we verify your email...</p>
            </div>
          </CardContent>
        </template>

        <!-- Success State -->
        <template v-else-if="success">
          <CardHeader>
            <CardTitle class="text-2xl flex items-center gap-2 text-green-600">
              <CheckCircle class="h-6 w-6" />
              Email Verified
            </CardTitle>
            <CardDescription>Your email has been successfully verified</CardDescription>
          </CardHeader>
          <CardContent>
            <Alert class="bg-green-50 border-green-200 text-green-700 mb-6">
              <CheckCircle class="h-4 w-4" />
              <AlertDescription class="ml-2">
                Your email address has been verified. You now have full access to your account.
              </AlertDescription>
            </Alert>
            <Button class="w-full" @click="navigateToLogin">
              Continue to Login
            </Button>
          </CardContent>
        </template>

        <!-- Error State -->
        <template v-else>
          <CardHeader>
            <CardTitle class="text-2xl flex items-center gap-2 text-red-600">
              <XCircle class="h-6 w-6" />
              Verification Failed
            </CardTitle>
            <CardDescription>We couldn't verify your email address</CardDescription>
          </CardHeader>
          <CardContent>
            <Alert variant="destructive" class="mb-6">
              <XCircle class="h-4 w-4" />
              <AlertDescription class="ml-2">
                {{ error }}
              </AlertDescription>
            </Alert>
            <p class="text-sm text-muted-foreground mb-6">
              If you continue to have issues, please log in to your account and request a new verification email from the Settings page.
            </p>
            <Button class="w-full" @click="navigateToLogin">
              Go to Login
            </Button>
          </CardContent>
        </template>
      </Card>

      <p class="mt-8 text-center text-sm text-muted-foreground">
        pennywise.cheap
      </p>
    </div>
  </div>
</template>
