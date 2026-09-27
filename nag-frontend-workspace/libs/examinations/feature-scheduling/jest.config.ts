export default {
  displayName: 'examinations-feature-scheduling',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/examinations/feature-scheduling',
  coverageThreshold: {
    global: {
      branches: 55,
      functions: 60,
      lines: 60,
      statements: 60,
    },
  },
};
