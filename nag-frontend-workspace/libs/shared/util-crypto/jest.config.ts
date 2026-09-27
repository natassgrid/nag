export default {
  displayName: 'shared-util-crypto',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/shared/util-crypto',
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80,
    },
  },
};
