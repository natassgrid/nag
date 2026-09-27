export default {
  displayName: 'admin-portal',
  preset: '../../jest.preset.js',
  coverageDirectory: '../../coverage/apps/admin-portal',
  coverageThreshold: {
    global: {
      branches: 50,
      functions: 50,
      lines: 50,
      statements: 50,
    },
  },
};
