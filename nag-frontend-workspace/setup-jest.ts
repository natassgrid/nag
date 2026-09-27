import 'zone.js';
import 'zone.js/testing';
import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';
import { TextEncoder, TextDecoder } from 'util';
import { webcrypto } from 'crypto';

Object.defineProperty(globalThis, 'TextEncoder', { value: TextEncoder, writable: true });
Object.defineProperty(globalThis, 'TextDecoder', { value: TextDecoder, writable: true });
Object.defineProperty(globalThis, 'crypto', { value: webcrypto, writable: true });

if (typeof window !== 'undefined') {
  Object.defineProperty(window, 'crypto', { value: webcrypto, writable: true });
}

setupZoneTestEnv();
