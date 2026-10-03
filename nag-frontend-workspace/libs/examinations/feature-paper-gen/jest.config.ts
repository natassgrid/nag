export default {
  displayName: 'examinations-feature-paper-gen',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/examinations/feature-paper-gen',
  coverageThreshold: {
    global: {
      branches: 55,
      functions: 60,
      lines: 60,
      statements: 60,
    },
  },
};
