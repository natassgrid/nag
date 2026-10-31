const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const projectRoot = __dirname;
const workspaceRoot = path.resolve(projectRoot, '../..');

const config = getDefaultConfig(projectRoot);

// 1. Watch all files in monorepo packages
config.watchFolders = [workspaceRoot];

// 2. Let Metro resolve node_modules in both project and workspace root
config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, 'node_modules'),
  path.resolve(workspaceRoot, 'node_modules'),
];

// 3. Resolve candidate-core package
config.resolver.extraNodeModules = {
  '@nag/candidate-core': path.resolve(workspaceRoot, 'packages/candidate-core'),
};

// 4. Force Metro to resolve subdependencies cleanly
config.resolver.disableHierarchicalLookup = true;

module.exports = config;
