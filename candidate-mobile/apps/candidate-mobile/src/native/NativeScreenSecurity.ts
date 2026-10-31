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

const getNativeModule = (): Spec | null => {
  if (typeof rnTurboModuleRegistry !== 'undefined' && rnTurboModuleRegistry.get) {
    const turbo = rnTurboModuleRegistry.get('NagScreenSecurityModule');
    if (turbo) return turbo;
  }
  if (rnNativeModules && rnNativeModules.NagScreenSecurityModule) {
    return rnNativeModules.NagScreenSecurityModule as Spec;
  }
  return null;
};

export const NativeScreenSecurity: Spec = {
  async enableFlagSecure(): Promise<ScreenSecurityStatus> {
    const native = getNativeModule();
    if (native && typeof native.enableFlagSecure === 'function') {
      return await native.enableFlagSecure();
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
      return await native.disableFlagSecure();
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
      return await native.getScreenSecurityStatus();
    }
    return {
      secure: simulatedSecure,
      timestamp: Date.now(),
    };
  },
};
