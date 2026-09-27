export default {
  displayName: 'evaluation-feature-grading',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/evaluation/feature-grading',
  coverageThreshold: {
    global: {
      branches: 55,
      functions: 60,
      lines: 60,
      statements: 60,
    },
  },
};
