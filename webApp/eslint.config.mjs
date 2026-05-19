// ESLint 9 flat config — permissivo all'inizio, sale di severità iterando.
import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import astro from 'eslint-plugin-astro';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';

export default [
  {
    ignores: [
      'dist/**',
      '.astro/**',
      'node_modules/**',
      'src/shims/**',
      'src/lib/sqldelight/**',
      'src/lib/__generated__/**',
      'public/**',
    ],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...astro.configs.recommended,
  {
    plugins: { 'react-hooks': reactHooks },
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.es2024,
      },
    },
    rules: {
      // Off al primo giro — alziamo nel tempo
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
      '@typescript-eslint/no-empty-function': 'off',
      '@typescript-eslint/ban-ts-comment': 'off',
      'no-empty': ['error', { allowEmptyCatch: true }],
      'no-constant-condition': ['error', { checkLoops: false }],
      'no-useless-escape': 'off',
      'no-prototype-builtins': 'off',
      'no-irregular-whitespace': 'off',
      'prefer-rest-params': 'off',

      // react-hooks: plugin registrato ma rules off (i commenti
      // `// eslint-disable-next-line react-hooks/exhaustive-deps` nel
      // codice non danno errore di "rule not found").
      'react-hooks/exhaustive-deps': 'off',
      'react-hooks/rules-of-hooks': 'off',
    },
  },
];
