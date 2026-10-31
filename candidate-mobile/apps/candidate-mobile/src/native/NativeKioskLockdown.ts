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

const getNativeModule = (): Spec | null => {
  if (typeof rnTurboModuleRegistry !== 'undefined' && rnTurboModuleRegistry.get) {
    const turbo = rnTurboModuleRegistry.get('NagKioskLockdownModule');
    if (turbo) return turbo;
  }
  if (rnNativeModules && rnNativeModules.NagKioskLockdownModule) {
    return rnNativeModules.NagKioskLockdownModule as Spec;
  }
  return null;
};

export const NativeKioskLockdown: Spec = {
  async startLockdown(): Promise<KioskStatus> {
    const native = getNativeModule();
    if (native && typeof native.startLockdown === 'function') {
      return await native.startLockdown();
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
      return await native.stopLockdown();
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
      return await native.getLockTaskModeState();
    }
    return { ...simulatedStatus };
  },

  async isDeviceOwner(): Promise<boolean> {
    const native = getNativeModule();
    if (native && typeof native.isDeviceOwner === 'function') {
      return await native.isDeviceOwner();
    }
    return simulatedStatus.isDeviceOwner;
  },

  async suppressOverlays(): Promise<boolean> {
    const native = getNativeModule();
    if (native && typeof native.suppressOverlays === 'function') {
      return await native.suppressOverlays();
    }
    simulatedStatus.overlaySuppressed = true;
    return true;
  },
};
