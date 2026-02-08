import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h, nextTick } from 'vue'
import CategoriesView from './CategoriesView.vue'
import { adminService } from '@/services/adminService'
import type { Category } from '@/types'

// Mock adminService
vi.mock('@/services/adminService', () => ({
  adminService: {
    getCategories: vi.fn(),
    createCategory: vi.fn(),
    updateCategory: vi.fn(),
    deleteCategory: vi.fn(),
  },
}))

// Mock child components
vi.mock('@/components/common/LoadingSpinner.vue', () => ({
  default: defineComponent({
    name: 'MockLoadingSpinner',
    props: { size: { type: String, default: undefined } },
    setup() {
      return () => h('div', { 'data-testid': 'loading-spinner' }, 'Loading...')
    },
  }),
}))

// Mock shadcn UI components
vi.mock('@/components/ui/card', () => ({
  Card: defineComponent({
    name: 'MockCard',
    setup(_, { slots }) {
      return () => h('div', { class: 'card' }, slots.default?.())
    },
  }),
  CardContent: defineComponent({
    name: 'MockCardContent',
    setup(_, { slots }) {
      return () => h('div', { class: 'card-content' }, slots.default?.())
    },
  }),
}))

vi.mock('@/components/ui/button', () => ({
  Button: defineComponent({
    name: 'MockButton',
    props: { variant: { type: String, default: undefined }, size: { type: String, default: undefined }, type: { type: String, default: undefined }, disabled: { type: Boolean, default: false } },
    emits: ['click'],
    setup(props, { slots, emit }) {
      return () => h('button', {
        type: props.type,
        disabled: props.disabled,
        onClick: () => emit('click'),
      }, slots.default?.())
    },
  }),
}))

vi.mock('@/components/ui/input', () => ({
  Input: defineComponent({
    name: 'MockInput',
    props: { modelValue: { type: [String, Number], default: undefined }, id: { type: String, default: undefined }, type: { type: String, default: undefined }, required: { type: Boolean, default: false }, placeholder: { type: String, default: undefined }, disabled: { type: Boolean, default: false } },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
      return () => h('input', {
        id: props.id,
        value: props.modelValue,
        type: props.type,
        disabled: props.disabled,
        onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLInputElement).value),
      })
    },
  }),
}))

vi.mock('@/components/ui/badge', () => ({
  Badge: defineComponent({
    name: 'MockBadge',
    props: { variant: { type: String, default: undefined } },
    setup(_, { slots }) {
      return () => h('span', { class: 'badge' }, slots.default?.())
    },
  }),
}))

vi.mock('@/components/ui/alert', () => ({
  Alert: defineComponent({
    name: 'MockAlert',
    props: { variant: { type: String, default: undefined } },
    setup(_, { slots }) {
      return () => h('div', { class: 'alert' }, slots.default?.())
    },
  }),
  AlertDescription: defineComponent({
    name: 'MockAlertDescription',
    setup(_, { slots }) {
      return () => h('div', slots.default?.())
    },
  }),
}))

vi.mock('lucide-vue-next', () => ({
  Tag: defineComponent({
    name: 'MockTag',
    setup() {
      return () => h('span', 'TagIcon')
    },
  }),
  Plus: defineComponent({
    name: 'MockPlus',
    setup() {
      return () => h('span', 'PlusIcon')
    },
  }),
  Pencil: defineComponent({
    name: 'MockPencil',
    setup() {
      return () => h('span', 'PencilIcon')
    },
  }),
  Trash2: defineComponent({
    name: 'MockTrash2',
    setup() {
      return () => h('span', 'Trash2Icon')
    },
  }),
}))

const mockCategories: Category[] = [
  { id: 1, name: 'Entertainment', system: true },
  { id: 2, name: 'Music', system: true },
  { id: 3, name: 'Custom Category', system: false },
]

describe('CategoriesView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should show loading spinner while fetching', async () => {
    vi.mocked(adminService.getCategories).mockReturnValue(new Promise(() => {}))

    const wrapper = mount(CategoriesView)
    await nextTick()

    expect(wrapper.find('[data-testid="loading-spinner"]').exists()).toBe(true)
  })

  it('should display categories after loading', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue(mockCategories)

    const wrapper = mount(CategoriesView)
    await flushPromises()

    expect(wrapper.text()).toContain('Entertainment')
    expect(wrapper.text()).toContain('Music')
    expect(wrapper.text()).toContain('Custom Category')
  })

  it('should display system and custom badges', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue(mockCategories)

    const wrapper = mount(CategoriesView)
    await flushPromises()

    expect(wrapper.text()).toContain('System')
    expect(wrapper.text()).toContain('Custom')
  })

  it('should show empty state when no categories exist', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue([])

    const wrapper = mount(CategoriesView)
    await flushPromises()

    expect(wrapper.text()).toContain('No categories')
    expect(wrapper.text()).toContain('Get started by adding your first category')
  })

  it('should show error message when fetch fails', async () => {
    vi.mocked(adminService.getCategories).mockRejectedValue({
      status: 500,
      message: 'Server error',
    })

    const wrapper = mount(CategoriesView)
    await flushPromises()

    expect(wrapper.text()).toContain('Server error')
  })

  it('should show page title and description', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue(mockCategories)

    const wrapper = mount(CategoriesView)
    await flushPromises()

    expect(wrapper.text()).toContain('Categories')
    expect(wrapper.text()).toContain('Manage service categories available to all users')
  })

  it('should show add category form when Add Category button is clicked', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue(mockCategories)

    const wrapper = mount(CategoriesView)
    await flushPromises()

    // Find and click the Add Category button
    const addButton = wrapper.findAll('button').find(b => b.text().includes('Add Category'))
    expect(addButton).toBeDefined()

    await addButton!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Category Name')
  })

  it('should create a new category', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue([...mockCategories])
    vi.mocked(adminService.createCategory).mockResolvedValue({ id: 4, name: 'Gaming', system: false })

    const wrapper = mount(CategoriesView)
    await flushPromises()

    // Open add form
    const addButton = wrapper.findAll('button').find(b => b.text().includes('Add Category'))
    await addButton!.trigger('click')
    await flushPromises()

    // Fill in the name
    const input = wrapper.find('input')
    await input.setValue('Gaming')
    await flushPromises()

    // Submit the form
    const form = wrapper.find('form')
    await form.trigger('submit')
    await flushPromises()

    expect(adminService.createCategory).toHaveBeenCalledWith({ name: 'Gaming' })
  })

  it('should show error when create fails', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue([...mockCategories])
    vi.mocked(adminService.createCategory).mockRejectedValue({
      status: 400,
      message: 'Category name already exists',
    })

    const wrapper = mount(CategoriesView)
    await flushPromises()

    // Open add form
    const addButton = wrapper.findAll('button').find(b => b.text().includes('Add Category'))
    await addButton!.trigger('click')
    await flushPromises()

    // Fill in the name
    const input = wrapper.find('input')
    await input.setValue('Entertainment')
    await flushPromises()

    // Submit the form
    const form = wrapper.find('form')
    await form.trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Category name already exists')
  })

  it('should delete a category after confirmation', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue([...mockCategories])
    vi.mocked(adminService.deleteCategory).mockResolvedValue(undefined)

    const wrapper = mount(CategoriesView)
    await flushPromises()

    // Click delete button (find Trash2Icon buttons)
    const deleteButtons = wrapper.findAll('button').filter(b => b.text().includes('Trash2Icon'))
    expect(deleteButtons.length).toBeGreaterThan(0)

    await deleteButtons[0].trigger('click')
    await flushPromises()

    // Confirm deletion
    expect(wrapper.text()).toContain('Delete?')
    const confirmButton = wrapper.findAll('button').find(b => b.text() === 'Confirm')
    expect(confirmButton).toBeDefined()

    await confirmButton!.trigger('click')
    await flushPromises()

    expect(adminService.deleteCategory).toHaveBeenCalledWith(1)
  })

  it('should show error when delete fails due to category in use', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue([...mockCategories])
    vi.mocked(adminService.deleteCategory).mockRejectedValue({
      status: 409,
      message: 'Category is in use and cannot be deleted',
    })

    const wrapper = mount(CategoriesView)
    await flushPromises()

    // Click delete button
    const deleteButtons = wrapper.findAll('button').filter(b => b.text().includes('Trash2Icon'))
    await deleteButtons[0].trigger('click')
    await flushPromises()

    // Confirm deletion
    const confirmButton = wrapper.findAll('button').find(b => b.text() === 'Confirm')
    await confirmButton!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Category is in use and cannot be deleted')
  })

  it('should cancel delete confirmation', async () => {
    vi.mocked(adminService.getCategories).mockResolvedValue([...mockCategories])

    const wrapper = mount(CategoriesView)
    await flushPromises()

    // Click delete button
    const deleteButtons = wrapper.findAll('button').filter(b => b.text().includes('Trash2Icon'))
    await deleteButtons[0].trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Delete?')

    // Cancel deletion
    const cancelButton = wrapper.findAll('button').find(b => b.text() === 'Cancel')
    await cancelButton!.trigger('click')
    await flushPromises()

    expect(adminService.deleteCategory).not.toHaveBeenCalled()
  })
})
