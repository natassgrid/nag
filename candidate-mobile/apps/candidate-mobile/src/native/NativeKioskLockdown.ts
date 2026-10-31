import type { NativeModulesStatic } from 'react-native';

export type LockTaskMode = 'NONE' | 'LOCKED' | 'PINNED';

export interface KioskStatus {
  isLockdownActive: boolean;
  lockTaskMode: LockTaskMode;
  isDeviceOwner: boolean;
  overlaySuppressed: boolean;
}

export interface Spec {
  startLockdown(): Promise<KioskStatus>;
  stopLockdown(): Promise<KioskStatus>;
  getLockTaskModeState(): Promise<KioskStatus>;
  isDeviceOwner(): Promise<boolean>;
  suppressOverlays(): Promise<boolean>;
}

// Fallback in-memory state for Dev / Testing / Web / Expo Go environments
let simulatedStatus: KioskStatus = {
  isLockdownActive: false,
  lockTaskMode: 'NONE',
  isDeviceOwner: false,
  overlaySuppressed: false,
};

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
  if (typeof (globalThis as any).NagKioskLockdownModule !== 'undefined') {
    return (globalThis as any).NagKioskLockdownModule;
  }
  if (typeof rnTurboModuleRegistry !== 'undefined' && rnTurboModuleRegistry.get) {
    const turbo = rnTurboModuleRegistry.get('NagKioskLockdownModule');
    if (turbo) return turbo;
  }
  if (rnNativeModules && rnNativeModules.NagKioskLockdownModule) {
    return rnNativeModules.NagKioskLockdownModule;
  }
  return null;
};

export const NativeKioskLockdown: Spec = {
  async startLockdown(): Promise<KioskStatus> {
    const native = getNativeModule();
    if (native && typeof native.startLockdown === 'function') {
      const res = await native.startLockdown();
      return typeof res === 'string' ? JSON.parse(res) : res;
    }
    simulatedStatus = {
      isLockdownActive: true,
      lockTaskMode: 'LOCKED',
      isDeviceOwner: true,
      overlaySuppressed: true,
    };
    return { ...simulatedStatus };
  },

  async stopLockdown(): Promise<KioskStatus> {
    const native = getNativeModule();
    if (native && typeof native.stopLockdown === 'function') {
      const res = await native.stopLockdown();
      return typeof res === 'string' ? JSON.parse(res) : res;
    }
    simulatedStatus = {
      isLockdownActive: false,
      lockTaskMode: 'NONE',
      isDeviceOwner: simulatedStatus.isDeviceOwner,
      overlaySuppressed: false,
    };
    return { ...simulatedStatus };
  },

  async getLockTaskModeState(): Promise<KioskStatus> {
    const native = getNativeModule();
    if (native && typeof native.getLockTaskModeState === 'function') {
      const res = await native.getLockTaskModeState();
      return typeof res === 'string' ? JSON.parse(res) : res;
    }
    return { ...simulatedStatus };
  },

  async isDeviceOwner(): Promise<boolean> {
    const native = getNativeModule();
    if (native && typeof native.isDeviceOwner === 'function') {
      const res = await native.isDeviceOwner();
      return typeof res === 'string' ? JSON.parse(res) : Boolean(res);
    }
    return simulatedStatus.isDeviceOwner;
  },

  async suppressOverlays(): Promise<boolean> {
    const native = getNativeModule();
    if (native && typeof native.suppressOverlays === 'function') {
      const res = await native.suppressOverlays();
      return typeof res === 'string' ? JSON.parse(res) : Boolean(res);
    }
    simulatedStatus.overlaySuppressed = true;
    return true;
  },
};
