export default {
  displayName: 'questions-data-access',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/questions/data-access',
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 75,
      lines: 75,
      statements: 75,
    },
  },
};
