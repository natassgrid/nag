/**
 * Ambient type definitions for React Native and Expo packages.
 * Ensures strict TypeScript compilation (tsc -p tsconfig.json --noEmit)
 * without requiring pre-installed heavyweight Android binary dependencies in CI.
 */

// Node/CJS globals
declare var require: any;
declare var module: any;
declare var exports: any;
declare var process: any;

// Global test runner types (Jest)
declare var describe: (name: string, fn: () => void) => void;
declare var it: (name: string, fn: () => void | Promise<any>) => void;
declare var test: (name: string, fn: () => void | Promise<any>) => void;
declare var expect: (actual: any) => any;
declare var beforeAll: (fn: () => void | Promise<any>) => void;
declare var afterAll: (fn: () => void | Promise<any>) => void;
declare var beforeEach: (fn: () => void | Promise<any>) => void;
declare var afterEach: (fn: () => void | Promise<any>) => void;

// Ambient JSX IntrinsicElements for React/React Native web components
declare namespace JSX {
  interface IntrinsicElements {
    [elemName: string]: any;
  }
}

declare namespace React {
  export const Fragment: any;
  export type FC<P = {}> = (props: P) => any;
  export type ReactNode = any;
  export type FormEvent<T = any> = any;
  export type ChangeEvent<T = any> = any;
  export type CSSProperties = Record<string, any>;
  export function useState<T>(initial: T | (() => T)): [T, (val: T | ((prev: T) => T)) => void];
  export function useEffect(effect: () => void | (() => void), deps?: any[]): void;
  export function useCallback<T extends (...args: any[]) => any>(callback: T, deps: any[]): T;
  export function useMemo<T>(factory: () => T, deps: any[] | undefined): T;
  export function useRef<T>(initial?: T): { current: T };
}

declare module 'react' {
  export const Fragment: any;
  export type FC<P = {}> = (props: P) => any;
  export type ReactNode = any;
  export type FormEvent<T = any> = any;
  export type ChangeEvent<T = any> = any;
  export type CSSProperties = Record<string, any>;
  export function useState<T>(initial: T | (() => T)): [T, (val: T | ((prev: T) => T)) => void];
  export function useEffect(effect: () => void | (() => void), deps?: any[]): void;
  export function useCallback<T extends (...args: any[]) => any>(callback: T, deps: any[]): T;
  export function useMemo<T>(factory: () => T, deps: any[] | undefined): T;
  export function useRef<T>(initial?: T): { current: T };
  const ReactDefault: typeof React;
  export default ReactDefault;
}

declare module 'react/jsx-runtime' {
  export const jsx: any;
  export const jsxs: any;
  export const Fragment: any;
}

declare module 'react-native' {
  export interface TurboModule {}

  export interface NativeModulesStatic {
    NagKioskLockdownModule?: any;
    NagScreenSecurityModule?: any;
    NagHardwareKeystoreModule?: any;
    NagProctoringCompanionModule?: any;
    [key: string]: any;
  }

  export const NativeModules: NativeModulesStatic;

  export namespace TurboModuleRegistry {
    export function get<T extends TurboModule>(name: string): T | null;
    export function getEnforcing<T extends TurboModule>(name: string): T;
  }

  export interface PlatformOSType {
    OS: 'ios' | 'android' | 'windows' | 'macos' | 'web';
    select<T>(specifics: { [platform in 'ios' | 'android' | 'native' | 'default']?: T }): T;
  }

  export const Platform: PlatformOSType;

  export interface EmitterSubscription {
    remove(): void;
  }

  export class NativeEventEmitter {
    constructor(nativeModule?: any);
    addListener(eventType: string, listener: (...args: any[]) => any): EmitterSubscription;
    removeAllListeners(eventType: string): void;
  }
}

declare module 'expo-secure-store' {
  export interface SecureStoreOptions {
    keychainService?: string;
    keychainAccessible?: number;
    requireAuthentication?: boolean;
    authenticationPrompt?: string;
  }

  export function setItemAsync(key: string, value: string, options?: SecureStoreOptions): Promise<void>;
  export function getItemAsync(key: string, options?: SecureStoreOptions): Promise<string | null>;
  export function deleteItemAsync(key: string, options?: SecureStoreOptions): Promise<void>;
  export function isAvailableAsync(): Promise<boolean>;
}

declare module 'expo-sqlite' {
  export interface SQLiteDatabase {
    execAsync(source: string): Promise<void>;
    runAsync(statement: string, ...params: any[]): Promise<{ lastInsertRowId: number; changes: number }>;
    getFirstAsync<T = any>(statement: string, ...params: any[]): Promise<T | null>;
    getAllAsync<T = any>(statement: string, ...params: any[]): Promise<T[]>;
    closeAsync(): Promise<void>;
  }

  export function openDatabaseSync(dbName: string): SQLiteDatabase;
  export function openDatabaseAsync(dbName: string): Promise<SQLiteDatabase>;
}

declare module 'expo-notifications' {
  export enum AndroidImportance {
    UNKNOWN = 0,
    UNSPECIFIED = 1,
    NONE = 2,
    MIN = 3,
    LOW = 4,
    DEFAULT = 5,
    HIGH = 6,
    MAX = 7,
  }

  export interface NotificationChannelInput {
    name: string;
    importance: AndroidImportance;
    sound?: string | boolean | null;
    vibrationPattern?: number[] | null;
    enableLights?: boolean;
    lightColor?: string;
    lockscreenVisibility?: number;
    bypassDnd?: boolean;
    showBadge?: boolean;
    description?: string;
  }

  export interface NotificationContentInput {
    title?: string | null;
    subtitle?: string | null;
    body?: string | null;
    data?: Record<string, any>;
    sound?: boolean | string;
    badge?: number;
  }

  export interface NotificationTriggerInput {
    channelId?: string;
    seconds?: number;
    date?: Date | number;
    repeats?: boolean;
  }

  export interface NotificationRequestInput {
    identifier?: string;
    content: NotificationContentInput;
    trigger: NotificationTriggerInput | null;
  }

  export interface NotificationPermissionsStatus {
    status: 'granted' | 'undetermined' | 'denied';
    granted: boolean;
    canAskAgain: boolean;
    expires: 'never' | number;
  }

  export function setNotificationChannelAsync(
    channelId: string,
    channel: NotificationChannelInput
  ): Promise<any>;

  export function scheduleNotificationAsync(
    request: NotificationRequestInput
  ): Promise<string>;

  export function cancelScheduledNotificationAsync(identifier: string): Promise<void>;
  export function getPermissionsAsync(): Promise<NotificationPermissionsStatus>;
  export function requestPermissionsAsync(): Promise<NotificationPermissionsStatus>;
}
