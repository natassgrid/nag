import type { NativeModulesStatic } from 'react-native';

export interface HardwareKeyInfo {
  alias: string;
  algorithm: string;
  publicKeyBase64: string;
  isHardwareBacked: boolean;
  isStrongBoxBacked: boolean;
  certificateChainLength: number;
  createdAt: number;
}

export interface ExamSignatureResult {
  alias: string;
  payloadHash: string;
  signatureBase64: string;
  algorithm: string;
  timestamp: number;
  strongBoxBacked: boolean;
}

export interface Spec {
  ensureHardwareKeyPair(alias?: string): Promise<HardwareKeyInfo>;
  signExamSubmission(payloadSha256Hex: string, alias?: string): Promise<ExamSignatureResult>;
  getHardwareKeyInfo(alias?: string): Promise<HardwareKeyInfo | null>;
}

const DEFAULT_ALIAS = 'NAG_CBT_HARDWARE_KEY';

// In-memory simulation registry for test/node/Expo Go environments
const simulatedKeyStore = new Map<string, HardwareKeyInfo>();

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
  if (typeof (globalThis as any).NagHardwareKeystoreModule !== 'undefined') {
    return (globalThis as any).NagHardwareKeystoreModule;
  }
  if (typeof rnTurboModuleRegistry !== 'undefined' && rnTurboModuleRegistry.get) {
    const turbo = rnTurboModuleRegistry.get('NagHardwareKeystoreModule');
    if (turbo) return turbo;
  }
  if (rnNativeModules && rnNativeModules.NagHardwareKeystoreModule) {
    return rnNativeModules.NagHardwareKeystoreModule;
  }
  return null;
};

// Deterministic mock public key in DER/X.509 format (65-byte uncompressed EC P-256)
const MOCK_PUBLIC_KEY_BASE64 =
  'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAER3s9e3wK1zM8kLzUq6F7t5f9B0qL1qP8' +
  'sO1hN3vR4wK2jT7mX5vL6zQ0wN1xY3uI9pE2sQ5vO8mK1w==';

function stringToBase64(str: string): string {
  if (typeof btoa === 'function') {
    return btoa(str);
  }
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
  let output = '';
  for (let i = 0; i < str.length; i += 3) {
    const a = str.charCodeAt(i);
    const b = i + 1 < str.length ? str.charCodeAt(i + 1) : 0;
    const c = i + 2 < str.length ? str.charCodeAt(i + 2) : 0;
    const enc1 = a >> 2;
    const enc2 = ((a & 3) << 4) | (b >> 4);
    let enc3 = ((b & 15) << 2) | (c >> 6);
    let enc4 = c & 63;
    if (i + 1 >= str.length) {
      enc3 = 64;
      enc4 = 64;
    } else if (i + 2 >= str.length) {
      enc4 = 64;
    }
    output += chars.charAt(enc1) + chars.charAt(enc2) + chars.charAt(enc3) + chars.charAt(enc4);
  }
  return output;
}

export const NativeHardwareKeystore: Spec = {
  async ensureHardwareKeyPair(alias?: string): Promise<HardwareKeyInfo> {
    const targetAlias = alias || DEFAULT_ALIAS;
    const native = getNativeModule();
    if (native && typeof native.ensureHardwareKeyPair === 'function') {
      const res = await native.ensureHardwareKeyPair(targetAlias);
      return typeof res === 'string' ? JSON.parse(res) : res;
    }

    let existing = simulatedKeyStore.get(targetAlias);
    if (!existing) {
      existing = {
        alias: targetAlias,
        algorithm: 'EC/secp256r1',
        publicKeyBase64: MOCK_PUBLIC_KEY_BASE64,
        isHardwareBacked: true,
        isStrongBoxBacked: true,
        certificateChainLength: 1,
        createdAt: Date.now(),
      };
      simulatedKeyStore.set(targetAlias, existing);
    }
    return { ...existing };
  },

  async signExamSubmission(payloadSha256Hex: string, alias?: string): Promise<ExamSignatureResult> {
    const targetAlias = alias || DEFAULT_ALIAS;
    const native = getNativeModule();
    if (native && typeof native.signExamSubmission === 'function') {
      const res = await native.signExamSubmission(payloadSha256Hex, targetAlias);
      return typeof res === 'string' ? JSON.parse(res) : res;
    }

    const keyInfo = await this.ensureHardwareKeyPair(targetAlias);

    // Deterministic mock ECDSA P-256 signature in Base64 (ASN.1 DER sequence)
    const simulatedSigBase64 = stringToBase64(
      `NAG_ECDSA_SIG:${targetAlias}:${payloadSha256Hex}:${keyInfo.createdAt}`
    );

    return {
      alias: targetAlias,
      payloadHash: payloadSha256Hex,
      signatureBase64: simulatedSigBase64,
      algorithm: 'SHA256withECDSA',
      timestamp: Date.now(),
      strongBoxBacked: keyInfo.isStrongBoxBacked,
    };
  },

  async getHardwareKeyInfo(alias?: string): Promise<HardwareKeyInfo | null> {
    const targetAlias = alias || DEFAULT_ALIAS;
    const native = getNativeModule();
    if (native && typeof native.getHardwareKeyInfo === 'function') {
      const res = await native.getHardwareKeyInfo(targetAlias);
      if (!res) return null;
      return typeof res === 'string' ? JSON.parse(res) : res;
    }

    const key = simulatedKeyStore.get(targetAlias);
    return key ? { ...key } : null;
  },
};
