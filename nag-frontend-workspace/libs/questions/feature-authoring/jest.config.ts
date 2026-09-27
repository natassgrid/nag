export default {
  displayName: 'questions-feature-authoring',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/questions/feature-authoring',
  coverageThreshold: {
    global: {
      branches: 60,
      functions: 65,
      lines: 65,
      statements: 65,
    },
  },
};
