module.exports = {
  env: {
    browser: true,
    es2021: true
  },
  extends: [
    'standard',
    'plugin:vue/vue3-recommended',
    './.eslintrc-auto-import.json'
  ],
  overrides: [
    {
      env: {
        node: true
      },
      files: [
        '.eslintrc.{js,cjs}'
      ],
      parserOptions: {
        sourceType: 'script'
      }
    }
  ],
  parserOptions: {
    ecmaVersion: 12,
    sourceType: 'module'
  },
  plugins: [
    'vue'
  ],
  rules: {
    'no-debugger': 'off',
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'off',
    'no-new-func': 'off',
    'vue/no-mutating-props': ['error', {
      shallowOnly: true
    }],
    'vue/one-component-per-file': 'off'
  },
  globals: {
    IS_NGINX: false
  }
}
