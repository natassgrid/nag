import type { NativeModulesStatic } from 'react-native';

export interface ScreenSecurityStatus {
  secure: boolean;
  timestamp: number;
}

export interface Spec {
  enableFlagSecure(): Promise<ScreenSecurityStatus>;
  disableFlagSecure(): Promise<ScreenSecurityStatus>;
  getScreenSecurityStatus(): Promise<ScreenSecurityStatus>;
}

// Fallback in-memory state for Dev / Testing / Web / Expo Go environments
let simulatedSecure = false;

let rnNativeModules: NativeModulesStatic = {} as any;
let rnTurboModuleRegistry: any = undefined;

try {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const rn = require('react-native');
  rnNativeModules = rn.NativeModules || {};
  rnTurboModuleRegistry = rn.TurboModuleRegistry;
} catch {
  // Headless test or dev fallback
}

const getNativeModule = (): any => {
  if (typeof (globalThis as any).NagScreenSecurityModule !== 'undefined') {
    return (globalThis as any).NagScreenSecurityModule;
  }
  if (typeof rnTurboModuleRegistry !== 'undefined' && rnTurboModuleRegistry.get) {
    const turbo = rnTurboModuleRegistry.get('NagScreenSecurityModule');
    if (turbo) return turbo;
  }
  if (rnNativeModules && rnNativeModules.NagScreenSecurityModule) {
    return rnNativeModules.NagScreenSecurityModule;
  }
  return null;
};

export const NativeScreenSecurity: Spec = {
  async enableFlagSecure(): Promise<ScreenSecurityStatus> {
    const native = getNativeModule();
    if (native && typeof native.enableFlagSecure === 'function') {
      const res = await native.enableFlagSecure();
      return typeof res === 'string' ? JSON.parse(res) : res;
    }
    simulatedSecure = true;
    return {
      secure: true,
      timestamp: Date.now(),
    };
  },

  async disableFlagSecure(): Promise<ScreenSecurityStatus> {
    const native = getNativeModule();
    if (native && typeof native.disableFlagSecure === 'function') {
      const res = await native.disableFlagSecure();
      return typeof res === 'string' ? JSON.parse(res) : res;
    }
    simulatedSecure = false;
    return {
      secure: false,
      timestamp: Date.now(),
    };
  },

  async getScreenSecurityStatus(): Promise<ScreenSecurityStatus> {
    const native = getNativeModule();
    if (native && typeof native.getScreenSecurityStatus === 'function') {
      const res = await native.getScreenSecurityStatus();
      return typeof res === 'string' ? JSON.parse(res) : res;
    }
    return {
      secure: simulatedSecure,
      timestamp: Date.now(),
    };
  },
};
