export default {
  displayName: 'evaluation-data-access',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/evaluation/data-access',
  coverageThreshold: {
    global: {
      branches: 75,
      functions: 75,
      lines: 75,
      statements: 75,
    },
  },
};
