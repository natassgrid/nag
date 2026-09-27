/**
 * WebCrypto utilities for cryptographic hashing, client-side encryption,
 * exam submission signature generation, and secure device fingerprinting.
 */

/**
 * Computes a SHA-256 hash of a string using the native WebCrypto API.
 */
export async function hashSha256(data: string): Promise<string> {
  const encoder = new TextEncoder();
  const dataBuffer = encoder.encode(data);
  const hashBuffer = await crypto.subtle.digest('SHA-256', dataBuffer);
  return bufferToHex(hashBuffer);
}

/**
 * Computes a SHA-512 hash of a string using the native WebCrypto API.
 */
export async function hashSha512(data: string): Promise<string> {
  const encoder = new TextEncoder();
  const dataBuffer = encoder.encode(data);
  const hashBuffer = await crypto.subtle.digest('SHA-512', dataBuffer);
  return bufferToHex(hashBuffer);
}

/**
 * Generates a cryptographically secure random hexadecimal salt.
 */
export function generateClientSalt(bytes = 16): string {
  const array = new Uint8Array(bytes);
  crypto.getRandomValues(array);
  return bufferToHex(array.buffer as ArrayBuffer);
}

/**
 * Generates a stable client device fingerprint for anti-tamper / proctoring sessions.
 */
export async function generateDeviceFingerprint(): Promise<string> {
  const components = [
    navigator.userAgent || '',
    navigator.language || '',
    screen.width + 'x' + screen.height,
    screen.colorDepth || '',
    Intl.DateTimeFormat().resolvedOptions().timeZone || '',
    navigator.hardwareConcurrency || '',
  ];
  return hashSha256(components.join(':::'));
}

/**
 * Encrypts a plaintext payload using AES-GCM (256-bit key).
 */
export async function encryptPayloadWebCrypto(
  payload: string,
  rawKeyHex: string
): Promise<{ cipherText: string; iv: string }> {
  const encoder = new TextEncoder();
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const keyBytes = hexToBuffer(rawKeyHex);

  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    keyBytes as unknown as BufferSource,
    { name: 'AES-GCM' },
    false,
    ['encrypt']
  );

  const encryptedBuffer = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv: iv as unknown as BufferSource },
    cryptoKey,
    encoder.encode(payload)
  );

  return {
    cipherText: bufferToHex(encryptedBuffer),
    iv: bufferToHex(iv.buffer as ArrayBuffer),
  };
}

/**
 * Decrypts an AES-GCM encrypted payload using the provided key and IV.
 */
export async function decryptPayloadWebCrypto(
  cipherTextHex: string,
  ivHex: string,
  rawKeyHex: string
): Promise<string> {
  const decoder = new TextDecoder();
  const iv = hexToBuffer(ivHex);
  const cipherBuffer = hexToBuffer(cipherTextHex);
  const keyBytes = hexToBuffer(rawKeyHex);

  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    keyBytes as unknown as BufferSource,
    { name: 'AES-GCM' },
    false,
    ['decrypt']
  );

  const decryptedBuffer = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: iv as unknown as BufferSource },
    cryptoKey,
    cipherBuffer as unknown as BufferSource
  );

  return decoder.decode(decryptedBuffer);
}

/**
 * Computes the tamper-evident candidate submission hash for immutable ledger recording.
 */
export async function signSubmissionHash(
  candidateIdOrHash: string,
  examId?: string,
  responsesJson?: string,
  timestampIso?: string
): Promise<string> {
  if (!examId) {
    return hashSha256(`${candidateIdOrHash}|SIGNATURE`);
  }
  const canonicalString = `${candidateIdOrHash}|${examId}|${timestampIso || ''}|${responsesJson || ''}`;
  return hashSha256(canonicalString);
}

function bufferToHex(buffer: ArrayBuffer): string {
  const byteView = new Uint8Array(buffer);
  let hex = '';
  for (let i = 0; i < byteView.length; i++) {
    hex += byteView[i].toString(16).padStart(2, '0');
  }
  return hex;
}

function hexToBuffer(hex: string): Uint8Array {
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < hex.length; i += 2) {
    bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
  }
  return bytes;
}
