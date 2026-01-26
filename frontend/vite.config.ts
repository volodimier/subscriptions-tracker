import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import csp from 'vite-plugin-csp-guard'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    csp({
      algorithm: 'sha256',
      dev: {
        run: false,
      },
      policy: {
        'default-src': ["'self'"],
        'script-src': ["'self'"],
        'script-src-elem': ["'self'", "'sha256-ZswfTY7H35rbv8WC7NXBoiC7WNu86vSzCDChNWwZZDM='"],
        'style-src': ["'self'", "'unsafe-inline'", 'https://fonts.googleapis.com'],
        'style-src-elem': ["'self'", 'https://fonts.googleapis.com'],
        'img-src': ["'self'", 'data:', 'https:'],
        'font-src': ["'self'", 'https://fonts.gstatic.com'],
        'connect-src': ["'self'"],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  build: {
    modulePreload: {
      polyfill: false,
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    include: ['src/**/*.{test,spec}.{js,ts}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov', 'json-summary'],
      reportsDirectory: './coverage',
      include: [
        'src/stores/**/*.ts',
        'src/utils/**/*.ts',
        'src/composables/**/*.ts',
        'src/services/**/*.ts',
      ],
      exclude: [
        'node_modules/',
        'src/**/*.test.ts',
        'src/**/*.spec.ts',
        'src/main.ts',
        'src/vite-env.d.ts',
        'src/services/api.ts',
      ],
    },
  },
})
