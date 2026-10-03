export default {
  displayName: 'candidate-delivery',
  preset: '../../jest.preset.js',
  coverageDirectory: '../../coverage/apps/candidate-delivery',
  coverageThreshold: {
    global: {
      branches: 50,
      functions: 50,
      lines: 50,
      statements: 50,
    },
  },
};
