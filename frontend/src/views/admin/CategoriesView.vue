<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { adminService, type AdminError } from '@/services/adminService'
import type { Category } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Tag, Plus, Pencil, Trash2 } from 'lucide-vue-next'

const categories = ref<Category[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const showAddForm = ref(false)
const newCategoryName = ref('')
const addLoading = ref(false)
const addError = ref<string | null>(null)

const editingId = ref<number | null>(null)
const editName = ref('')
const editLoading = ref(false)
const editError = ref<string | null>(null)

const deletingId = ref<number | null>(null)
const deleteLoading = ref(false)
const deleteError = ref<string | null>(null)

const hasCategories = computed(() => categories.value.length > 0)

async function fetchCategories() {
  loading.value = true
  error.value = null
  try {
    categories.value = await adminService.getCategories()
  } catch (err) {
    const adminErr = err as AdminError
    error.value = adminErr.message || 'Failed to fetch categories'
  } finally {
    loading.value = false
  }
}

function openAddForm() {
  showAddForm.value = true
  newCategoryName.value = ''
  addError.value = null
}

function cancelAdd() {
  showAddForm.value = false
  newCategoryName.value = ''
  addError.value = null
}

async function handleAdd() {
  const trimmed = newCategoryName.value.trim()
  if (!trimmed) return

  addLoading.value = true
  addError.value = null
  try {
    const created = await adminService.createCategory({ name: trimmed })
    categories.value.push(created)
    cancelAdd()
  } catch (err) {
    const adminErr = err as AdminError
    addError.value = adminErr.message || 'Failed to create category'
  } finally {
    addLoading.value = false
  }
}

function startEdit(category: Category) {
  editingId.value = category.id
  editName.value = category.name
  editError.value = null
}

function cancelEdit() {
  editingId.value = null
  editName.value = ''
  editError.value = null
}

async function handleEdit(id: number) {
  const trimmed = editName.value.trim()
  if (!trimmed) return

  editLoading.value = true
  editError.value = null
  try {
    const updated = await adminService.updateCategory(id, { name: trimmed })
    const index = categories.value.findIndex(c => c.id === id)
    if (index !== -1) {
      categories.value[index] = updated
    }
    cancelEdit()
  } catch (err) {
    const adminErr = err as AdminError
    editError.value = adminErr.message || 'Failed to update category'
  } finally {
    editLoading.value = false
  }
}

function confirmDelete(id: number) {
  deletingId.value = id
  deleteError.value = null
}

function cancelDelete() {
  deletingId.value = null
  deleteError.value = null
}

async function handleDelete(id: number) {
  deleteLoading.value = true
  deleteError.value = null
  try {
    await adminService.deleteCategory(id)
    categories.value = categories.value.filter(c => c.id !== id)
    cancelDelete()
  } catch (err) {
    const adminErr = err as AdminError
    deleteError.value = adminErr.message || 'Failed to delete category'
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  fetchCategories()
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Categories</h1>
        <p class="mt-1 text-sm text-muted-foreground">Manage service categories available to all users</p>
      </div>
      <Button v-if="!showAddForm" @click="openAddForm">
        <Plus class="h-4 w-4 mr-2" />
        Add Category
      </Button>
    </div>

    <!-- Error Message -->
    <Alert v-if="error" variant="destructive" class="mb-6">
      <AlertDescription>{{ error }}</AlertDescription>
    </Alert>

    <!-- Add Category Form -->
    <Card v-if="showAddForm" class="mb-6">
      <CardContent class="p-4">
        <Alert v-if="addError" variant="destructive" class="mb-4">
          <AlertDescription>{{ addError }}</AlertDescription>
        </Alert>
        <form class="flex items-end gap-3" @submit.prevent="handleAdd">
          <div class="flex-1">
            <label for="new-category" class="block text-sm font-medium text-gray-700 mb-1">Category Name</label>
            <Input
              id="new-category"
              v-model="newCategoryName"
              type="text"
              placeholder="Enter category name"
              required
              :disabled="addLoading"
            />
          </div>
          <Button type="submit" :disabled="!newCategoryName.trim() || addLoading">
            {{ addLoading ? 'Adding...' : 'Add' }}
          </Button>
          <Button type="button" variant="outline" :disabled="addLoading" @click="cancelAdd">
            Cancel
          </Button>
        </form>
      </CardContent>
    </Card>

    <!-- Loading State -->
    <div v-if="loading && !hasCategories" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <!-- Empty State -->
    <div
      v-else-if="!loading && !hasCategories && !error"
      class="text-center py-12"
    >
      <Tag class="mx-auto h-12 w-12 text-gray-400" />
      <h3 class="mt-2 text-sm font-medium text-gray-900">No categories</h3>
      <p class="mt-1 text-sm text-muted-foreground">Get started by adding your first category.</p>
    </div>

    <!-- Categories Table -->
    <Card v-else-if="hasCategories" class="overflow-hidden">
      <CardContent class="p-0">
        <!-- Delete Error -->
        <Alert v-if="deleteError" variant="destructive" class="m-4">
          <AlertDescription>{{ deleteError }}</AlertDescription>
        </Alert>

        <!-- Desktop Table -->
        <div class="hidden md:block overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-muted/50">
              <tr>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Name
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Type
                </th>
                <th scope="col" class="px-6 py-3 text-right text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody class="bg-background divide-y divide-gray-200">
              <tr v-for="category in categories" :key="category.id" class="hover:bg-muted/50">
                <td class="px-6 py-4 whitespace-nowrap">
                  <template v-if="editingId === category.id">
                    <div class="flex items-center gap-2">
                      <Input
                        v-model="editName"
                        type="text"
                        class="max-w-xs"
                        :disabled="editLoading"
                        @keydown.enter.prevent="handleEdit(category.id)"
                        @keydown.escape="cancelEdit"
                      />
                      <Button size="sm" :disabled="!editName.trim() || editLoading" @click="handleEdit(category.id)">
                        {{ editLoading ? 'Saving...' : 'Save' }}
                      </Button>
                      <Button size="sm" variant="outline" :disabled="editLoading" @click="cancelEdit">
                        Cancel
                      </Button>
                    </div>
                    <p v-if="editError" class="text-sm text-red-600 mt-1">{{ editError }}</p>
                  </template>
                  <template v-else>
                    <span class="text-sm font-medium text-gray-900">{{ category.name }}</span>
                  </template>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <Badge
                    variant="outline"
                    :class="category.system ? 'bg-blue-50 text-blue-800 border-blue-200' : 'bg-gray-50 text-gray-800 border-gray-200'"
                  >
                    {{ category.system ? 'System' : 'Custom' }}
                  </Badge>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right">
                  <template v-if="deletingId === category.id">
                    <div class="flex items-center justify-end gap-2">
                      <span class="text-sm text-gray-600">Delete?</span>
                      <Button size="sm" variant="destructive" :disabled="deleteLoading" @click="handleDelete(category.id)">
                        {{ deleteLoading ? 'Deleting...' : 'Confirm' }}
                      </Button>
                      <Button size="sm" variant="outline" :disabled="deleteLoading" @click="cancelDelete">
                        Cancel
                      </Button>
                    </div>
                  </template>
                  <template v-else-if="editingId !== category.id">
                    <div class="flex items-center justify-end gap-2">
                      <Button size="sm" variant="ghost" @click="startEdit(category)">
                        <Pencil class="h-4 w-4" />
                        <span class="sr-only">Edit</span>
                      </Button>
                      <Button size="sm" variant="ghost" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="confirmDelete(category.id)">
                        <Trash2 class="h-4 w-4" />
                        <span class="sr-only">Delete</span>
                      </Button>
                    </div>
                  </template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Mobile Cards -->
        <div class="md:hidden divide-y divide-gray-200">
          <div v-for="category in categories" :key="category.id" class="p-4 space-y-3">
            <template v-if="editingId === category.id">
              <div class="space-y-2">
                <Input
                  v-model="editName"
                  type="text"
                  :disabled="editLoading"
                  @keydown.enter.prevent="handleEdit(category.id)"
                  @keydown.escape="cancelEdit"
                />
                <p v-if="editError" class="text-sm text-red-600">{{ editError }}</p>
                <div class="flex gap-2">
                  <Button size="sm" :disabled="!editName.trim() || editLoading" @click="handleEdit(category.id)">
                    {{ editLoading ? 'Saving...' : 'Save' }}
                  </Button>
                  <Button size="sm" variant="outline" :disabled="editLoading" @click="cancelEdit">
                    Cancel
                  </Button>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="flex items-center justify-between">
                <span class="font-medium text-gray-900">{{ category.name }}</span>
                <Badge
                  variant="outline"
                  :class="category.system ? 'bg-blue-50 text-blue-800 border-blue-200' : 'bg-gray-50 text-gray-800 border-gray-200'"
                >
                  {{ category.system ? 'System' : 'Custom' }}
                </Badge>
              </div>
              <template v-if="deletingId === category.id">
                <Alert v-if="deleteError" variant="destructive">
                  <AlertDescription>{{ deleteError }}</AlertDescription>
                </Alert>
                <div class="flex items-center gap-2">
                  <span class="text-sm text-gray-600">Delete this category?</span>
                  <Button size="sm" variant="destructive" :disabled="deleteLoading" @click="handleDelete(category.id)">
                    {{ deleteLoading ? 'Deleting...' : 'Confirm' }}
                  </Button>
                  <Button size="sm" variant="outline" :disabled="deleteLoading" @click="cancelDelete">
                    Cancel
                  </Button>
                </div>
              </template>
              <div v-else class="flex gap-2">
                <Button size="sm" variant="outline" @click="startEdit(category)">
                  <Pencil class="h-4 w-4 mr-1" />
                  Edit
                </Button>
                <Button size="sm" variant="outline" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="confirmDelete(category.id)">
                  <Trash2 class="h-4 w-4 mr-1" />
                  Delete
                </Button>
              </div>
            </template>
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
