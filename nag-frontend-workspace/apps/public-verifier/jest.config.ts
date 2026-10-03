export default {
  displayName: 'public-verifier',
  preset: '../../jest.preset.js',
  coverageDirectory: '../../coverage/apps/public-verifier',
  coverageThreshold: {
    global: {
      branches: 50,
      functions: 50,
      lines: 50,
      statements: 50,
    },
  },
};
