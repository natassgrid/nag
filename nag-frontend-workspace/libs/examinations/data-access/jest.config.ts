export default {
  displayName: 'examinations-data-access',
  preset: '../../../jest.preset.js',
  coverageDirectory: '../../../coverage/libs/examinations/data-access',
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 75,
      lines: 75,
      statements: 75,
    },
  },
};
