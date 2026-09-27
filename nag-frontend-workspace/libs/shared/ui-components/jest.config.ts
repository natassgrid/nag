export default {
  displayName: 'shared-ui-components',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/shared/ui-components',
  coverageThreshold: {
    global: {
      branches: 75,
      functions: 75,
      lines: 75,
      statements: 75,
    },
  },
};
