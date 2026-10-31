/** @type {import('jest').Config} */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: [
    '<rootDir>/packages/candidate-core/src',
    '<rootDir>/apps/candidate-mobile/src',
  ],
  testMatch: ['**/__tests__/**/*.spec.ts', '**/__tests__/**/*.spec.tsx'],
  moduleNameMapper: {
    '^@nag/candidate-core$': '<rootDir>/packages/candidate-core/src/index.ts',
    '^@nag/candidate-core/(.*)$': '<rootDir>/packages/candidate-core/src/$1',
  },
  transform: {
    '^.+\\.tsx?$': [
      'ts-jest',
      {
        tsconfig: '<rootDir>/tsconfig.base.json',
      },
    ],
  },
  collectCoverageFrom: [
    'packages/candidate-core/src/**/*.ts',
    'apps/candidate-mobile/src/**/*.{ts,tsx}',
    '!**/index.ts',
    '!**/__tests__/**',
  ],
};
