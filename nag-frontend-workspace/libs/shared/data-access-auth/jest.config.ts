export default {
  displayName: 'shared-data-access-auth',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/shared/data-access-auth',
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80,
    },
  },
};
