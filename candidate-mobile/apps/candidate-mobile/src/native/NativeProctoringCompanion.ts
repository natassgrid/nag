import type { NativeModulesStatic } from 'react-native';

export interface SensorSample {
  ambientNoiseDb: number;
  pitchDeg: number;
  rollDeg: number;
  azimuthDeg: number;
  noiseViolation: boolean;
  postureViolation: boolean;
  timestamp: number;
}

export interface Spec {
  startSensorMonitoring(
    audioThresholdDb?: number,
    orientationThresholdDeg?: number
  ): Promise<boolean>;
  stopSensorMonitoring(): Promise<boolean>;
  evaluateSensorSample(sample?: Partial<SensorSample>): Promise<SensorSample>;
}

export const DEFAULT_AUDIO_THRESHOLD_DB = 65.0;
export const DEFAULT_ORIENTATION_THRESHOLD_DEG = 45.0;

// Fallback in-memory simulation state
let simulatedMonitoring = false;
let configuredAudioThreshold = DEFAULT_AUDIO_THRESHOLD_DB;
let configuredOrientationThreshold = DEFAULT_ORIENTATION_THRESHOLD_DEG;

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
  if (typeof (globalThis as any).NagProctoringCompanionModule !== 'undefined') {
    return (globalThis as any).NagProctoringCompanionModule;
  }
  if (typeof rnTurboModuleRegistry !== 'undefined' && rnTurboModuleRegistry.get) {
    const turbo = rnTurboModuleRegistry.get('NagProctoringCompanionModule');
    if (turbo) return turbo;
  }
  if (rnNativeModules && rnNativeModules.NagProctoringCompanionModule) {
    return rnNativeModules.NagProctoringCompanionModule;
  }
  return null;
};

export const NativeProctoringCompanion: Spec = {
  async startSensorMonitoring(
    audioThresholdDb: number = DEFAULT_AUDIO_THRESHOLD_DB,
    orientationThresholdDeg: number = DEFAULT_ORIENTATION_THRESHOLD_DEG
  ): Promise<boolean> {
    configuredAudioThreshold = audioThresholdDb > 0 ? audioThresholdDb : DEFAULT_AUDIO_THRESHOLD_DB;
    configuredOrientationThreshold =
      orientationThresholdDeg > 0 ? orientationThresholdDeg : DEFAULT_ORIENTATION_THRESHOLD_DEG;

    const native = getNativeModule();
    if (native && typeof native.startSensorMonitoring === 'function') {
      const res = await native.startSensorMonitoring(configuredAudioThreshold, configuredOrientationThreshold);
      return typeof res === 'string' ? JSON.parse(res) : Boolean(res);
    }

    simulatedMonitoring = true;
    return true;
  },

  async stopSensorMonitoring(): Promise<boolean> {
    const native = getNativeModule();
    if (native && typeof native.stopSensorMonitoring === 'function') {
      const res = await native.stopSensorMonitoring();
      return typeof res === 'string' ? JSON.parse(res) : Boolean(res);
    }

    simulatedMonitoring = false;
    return true;
  },

  async evaluateSensorSample(sample?: Partial<SensorSample>): Promise<SensorSample> {
    const native = getNativeModule();
    if (!sample && native && typeof native.evaluateSensorSample === 'function') {
      const res = await native.evaluateSensorSample();
      return typeof res === 'string' ? JSON.parse(res) : res;
    }

    // Evaluate provided sample or deterministic baseline
    const ambientNoiseDb = typeof sample?.ambientNoiseDb === 'number' ? sample.ambientNoiseDb : 42.5;
    const pitchDeg = typeof sample?.pitchDeg === 'number' ? sample.pitchDeg : 12.0;
    const rollDeg = typeof sample?.rollDeg === 'number' ? sample.rollDeg : 5.0;
    const azimuthDeg = typeof sample?.azimuthDeg === 'number' ? sample.azimuthDeg : 180.0;

    const noiseViolation = ambientNoiseDb > configuredAudioThreshold;
    const postureViolation =
      Math.abs(pitchDeg) > configuredOrientationThreshold ||
      Math.abs(rollDeg) > configuredOrientationThreshold;

    return {
      ambientNoiseDb: Math.round(ambientNoiseDb * 10) / 10,
      pitchDeg: Math.round(pitchDeg * 10) / 10,
      rollDeg: Math.round(rollDeg * 10) / 10,
      azimuthDeg: Math.round(azimuthDeg * 10) / 10,
      noiseViolation,
      postureViolation,
      timestamp: sample?.timestamp || Date.now(),
    };
  },
};
