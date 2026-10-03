export default {
  displayName: 'questions-ui-question-card',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/questions/ui-question-card',
  coverageThreshold: {
    global: {
      branches: 75,
      functions: 75,
      lines: 75,
      statements: 75,
    },
  },
};
