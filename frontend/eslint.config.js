import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import globals from 'globals'

export default tseslint.config(
  // Global ignores
  {
    ignores: [
      'dist/**',
      'coverage/**',
      'node_modules/**',
      '*.config.js',
      '*.config.ts',
    ],
  },

  // Base JavaScript recommended rules
  js.configs.recommended,

  // TypeScript recommended rules
  ...tseslint.configs.recommended,

  // Vue 3 recommended rules
  ...pluginVue.configs['flat/recommended'],

  // Global settings for all files
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.es2020,
        ...globals.node,
      },
      ecmaVersion: 2020,
      sourceType: 'module',
    },
  },

  // TypeScript-specific settings
  {
    files: ['**/*.ts', '**/*.tsx'],
    languageOptions: {
      parser: tseslint.parser,
      parserOptions: {
        ecmaVersion: 2020,
        sourceType: 'module',
      },
    },
    rules: {
      // Allow unused vars with underscore prefix (common pattern for intentionally unused)
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      // Allow explicit any in some cases (warn instead of error for gradual migration)
      '@typescript-eslint/no-explicit-any': 'warn',
    },
  },

  // Vue-specific settings
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: pluginVue.parser,
      parserOptions: {
        parser: tseslint.parser,
        ecmaVersion: 2020,
        sourceType: 'module',
      },
    },
    rules: {
      // Allow unused vars with underscore prefix
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      // Allow explicit any in some cases
      '@typescript-eslint/no-explicit-any': 'warn',
      // Allow single-word component names (common in Vue projects)
      'vue/multi-word-component-names': 'off',
      // Allow v-html when needed (developer should ensure safe content)
      'vue/no-v-html': 'warn',
      // Enforce consistent attribute order but don't be too strict
      'vue/attributes-order': 'warn',
      // Allow self-closing for all tags
      'vue/html-self-closing': [
        'warn',
        {
          html: {
            void: 'always',
            normal: 'always',
            component: 'always',
          },
          svg: 'always',
          math: 'always',
        },
      ],
      // Allow slightly longer lines in templates
      'vue/max-attributes-per-line': 'off',
      // Allow attributes on same line as tag name
      'vue/first-attribute-linebreak': 'off',
      // Don't require closing bracket on new line
      'vue/html-closing-bracket-newline': 'off',
      // Relax singleline html element content newline requirement
      'vue/singleline-html-element-content-newline': 'off',
    },
  },

  // Test files settings
  {
    files: ['**/*.test.ts', '**/*.spec.ts', '**/__tests__/**/*.ts'],
    languageOptions: {
      globals: {
        ...globals.jest,
        describe: 'readonly',
        it: 'readonly',
        expect: 'readonly',
        beforeEach: 'readonly',
        afterEach: 'readonly',
        beforeAll: 'readonly',
        afterAll: 'readonly',
        vi: 'readonly',
      },
    },
    rules: {
      // Allow any in tests for mocking purposes
      '@typescript-eslint/no-explicit-any': 'off',
    },
  }
)
