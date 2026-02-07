import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h, nextTick } from 'vue'
import { provideSidebar, useSidebar } from './useSidebar'

// Mock useMediaQuery from @vueuse/core
const mockIsMobile = { value: false }

vi.mock('@vueuse/core', () => ({
  useMediaQuery: () => mockIsMobile,
}))

// Create a wrapper component that provides the sidebar context
const createWrapperComponent = () => {
  // eslint-disable-next-line vue/one-component-per-file
  return defineComponent({
    setup() {
      const context = provideSidebar()
      return { context }
    },
    render() {
      return h('div', {}, this.$slots.default?.())
    },
  })
}

// Create a child component that uses the sidebar
// eslint-disable-next-line vue/one-component-per-file
const ChildComponent = defineComponent({
  setup() {
    const sidebar = useSidebar()
    return { sidebar }
  },
  render() {
    return h('div', { 'data-testid': 'child' })
  },
})

describe('useSidebar', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear()
    // Reset mobile state
    mockIsMobile.value = false
  })

  describe('provideSidebar', () => {
    it('should provide sidebar context', () => {
      const WrapperComponent = createWrapperComponent()
      const wrapper = mount(WrapperComponent)

      expect(wrapper.vm.context).toBeDefined()
      expect(wrapper.vm.context.isCollapsed).toBeDefined()
      expect(wrapper.vm.context.isMobile).toBeDefined()
      expect(wrapper.vm.context.isMobileOpen).toBeDefined()
      expect(wrapper.vm.context.toggle).toBeDefined()
      expect(wrapper.vm.context.setCollapsed).toBeDefined()
      expect(wrapper.vm.context.openMobile).toBeDefined()
      expect(wrapper.vm.context.closeMobile).toBeDefined()
    })

    it('should initialize isCollapsed from localStorage', () => {
      localStorage.setItem('sidebar-collapsed', 'true')

      const WrapperComponent = createWrapperComponent()
      const wrapper = mount(WrapperComponent)

      expect(wrapper.vm.context.isCollapsed.value).toBe(true)
    })

    it('should initialize isCollapsed as false when localStorage is empty', () => {
      const WrapperComponent = createWrapperComponent()
      const wrapper = mount(WrapperComponent)

      expect(wrapper.vm.context.isCollapsed.value).toBe(false)
    })
  })

  describe('toggle', () => {
    it('should toggle isCollapsed on desktop', async () => {
      mockIsMobile.value = false

      const WrapperComponent = createWrapperComponent()
      const wrapper = mount(WrapperComponent)

      expect(wrapper.vm.context.isCollapsed.value).toBe(false)

      wrapper.vm.context.toggle()
      await nextTick()

      expect(wrapper.vm.context.isCollapsed.value).toBe(true)
      expect(localStorage.getItem('sidebar-collapsed')).toBe('true')
    })

    it('should toggle isMobileOpen on mobile', async () => {
      mockIsMobile.value = true

      const WrapperComponent = createWrapperComponent()
      const wrapper = mount(WrapperComponent)

      expect(wrapper.vm.context.isMobileOpen.value).toBe(false)

      wrapper.vm.context.toggle()
      await nextTick()

      expect(wrapper.vm.context.isMobileOpen.value).toBe(true)
    })
  })

  describe('setCollapsed', () => {
    it('should set isCollapsed to true', async () => {
      const WrapperComponent = createWrapperComponent()
      const wrapper = mount(WrapperComponent)

      wrapper.vm.context.setCollapsed(true)
      await nextTick()

      expect(wrapper.vm.context.isCollapsed.value).toBe(true)
      expect(localStorage.getItem('sidebar-collapsed')).toBe('true')
    })

    it('should set isCollapsed to false', async () => {
      localStorage.setItem('sidebar-collapsed', 'true')

      const WrapperComponent = createWrapperComponent()
      const wrapper = mount(WrapperComponent)

      wrapper.vm.context.setCollapsed(false)
      await nextTick()

      expect(wrapper.vm.context.isCollapsed.value).toBe(false)
      expect(localStorage.getItem('sidebar-collapsed')).toBe('false')
    })
  })

  describe('openMobile and closeMobile', () => {
    it('should open mobile sidebar', async () => {
      const WrapperComponent = createWrapperComponent()
      const wrapper = mount(WrapperComponent)

      wrapper.vm.context.openMobile()
      await nextTick()

      expect(wrapper.vm.context.isMobileOpen.value).toBe(true)
    })

    it('should close mobile sidebar', async () => {
      const WrapperComponent = createWrapperComponent()
      const wrapper = mount(WrapperComponent)

      wrapper.vm.context.openMobile()
      await nextTick()
      expect(wrapper.vm.context.isMobileOpen.value).toBe(true)

      wrapper.vm.context.closeMobile()
      await nextTick()
      expect(wrapper.vm.context.isMobileOpen.value).toBe(false)
    })
  })

  describe('useSidebar without provider', () => {
    it('should throw error when used outside provider', () => {
      // eslint-disable-next-line vue/one-component-per-file
      const ComponentWithoutProvider = defineComponent({
        setup() {
          useSidebar()
        },
        render() {
          return h('div')
        },
      })

      expect(() => mount(ComponentWithoutProvider)).toThrow(
        'useSidebar must be used within a component that calls provideSidebar'
      )
    })
  })

  describe('useSidebar with provider', () => {
    it('should access sidebar context from child component', () => {
      const WrapperComponent = createWrapperComponent()

      const wrapper = mount(WrapperComponent, {
        slots: {
          default: () => h(ChildComponent),
        },
      })

      const child = wrapper.findComponent(ChildComponent)
      expect(child.vm.sidebar).toBeDefined()
      expect(child.vm.sidebar.isCollapsed).toBeDefined()
    })
  })
})
