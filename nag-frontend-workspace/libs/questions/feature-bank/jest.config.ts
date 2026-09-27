export default {
  displayName: 'questions-feature-bank',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/questions/feature-bank',
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 70,
      lines: 70,
      statements: 70,
    },
  },
};
