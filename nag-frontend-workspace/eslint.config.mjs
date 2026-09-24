import nx from '@nx/eslint-plugin';

export default [
  ...nx.configs['flat/base'],
  ...nx.configs['flat/typescript'],
  ...nx.configs['flat/javascript'],
  {
    ignores: ['**/dist', '**/out-tsc'],
  },
  {
    files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx'],
    rules: {
      '@nx/enforce-module-boundaries': [
        'error',
        {
          enforceBuildableLibDependency: true,
          allow: ['^.*/eslint(\\.base)?\\.config\\.[cm]?[jt]s$'],
          depConstraints: [
            // Layer architectural rules (unidirectional flow: feature -> data-access / ui -> util)
            {
              sourceTag: 'type:app',
              onlyDependOnLibsWithTags: [
                'type:feature',
                'type:ui',
                'type:data-access',
                'type:util',
              ],
            },
            {
              sourceTag: 'type:feature',
              onlyDependOnLibsWithTags: [
                'type:feature',
                'type:ui',
                'type:data-access',
                'type:util',
              ],
            },
            {
              sourceTag: 'type:ui',
              onlyDependOnLibsWithTags: ['type:ui', 'type:util'],
            },
            {
              sourceTag: 'type:data-access',
              onlyDependOnLibsWithTags: ['type:data-access', 'type:util'],
            },
            {
              sourceTag: 'type:util',
              onlyDependOnLibsWithTags: ['type:util'],
            },

            // Domain Scope isolation rules
            {
              sourceTag: 'scope:shared',
              onlyDependOnLibsWithTags: ['scope:shared'],
            },
            {
              sourceTag: 'scope:questions',
              onlyDependOnLibsWithTags: ['scope:questions', 'scope:shared'],
            },
            {
              sourceTag: 'scope:examinations',
              onlyDependOnLibsWithTags: [
                'scope:examinations',
                'scope:questions',
                'scope:shared',
              ],
            },
            {
              sourceTag: 'scope:evaluation',
              onlyDependOnLibsWithTags: [
                'scope:evaluation',
                'scope:questions',
                'scope:examinations',
                'scope:shared',
              ],
            },
            {
              sourceTag: 'scope:admin',
              onlyDependOnLibsWithTags: [
                'scope:admin',
                'scope:questions',
                'scope:examinations',
                'scope:evaluation',
                'scope:shared',
              ],
            },
            {
              sourceTag: 'scope:candidate',
              onlyDependOnLibsWithTags: [
                'scope:candidate',
                'scope:questions',
                'scope:examinations',
                'scope:shared',
              ],
            },
            {
              sourceTag: 'scope:verifier',
              onlyDependOnLibsWithTags: [
                'scope:verifier',
                'scope:examinations',
                'scope:shared',
              ],
            },
          ],
        },
      ],
    },
  },
  {
    files: [
      '**/*.ts',
      '**/*.tsx',
      '**/*.cts',
      '**/*.mts',
      '**/*.js',
      '**/*.jsx',
      '**/*.cjs',
      '**/*.mjs',
    ],
    rules: {},
  },
];
