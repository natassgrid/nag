/**
 * NAG Cryptographic QR Verifier & Offline Admit Card Digest Utility
 * Works natively on Node.js, Web, and React Native Hermes without external dependencies.
 */

import {
  DigitalAdmitCard,
  QrVerificationResult,
  SignedQrPayload,
} from '../models';

// ============================================================================
// Pure TypeScript Zero-Dependency SHA-256 Implementation
// ============================================================================

const SHA256_K = [
  0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
  0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
  0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
  0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
  0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
  0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
  0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
  0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f, 0xc67178f2,
];

function ror(val: number, amt: number): number {
  return (val >>> amt) | (val << (32 - amt));
}

function pureSha256(ascii: string): string {
  // Convert utf8 string to byte array
  const bytes: number[] = [];
  for (let i = 0; i < ascii.length; i++) {
    let code = ascii.charCodeAt(i);
    if (code < 0x80) {
      bytes.push(code);
    } else if (code < 0x800) {
      bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f));
    } else if (code < 0xd800 || code >= 0xe000) {
      bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
    } else {
      i++;
      code = 0x10000 + (((code & 0x3ff) << 10) | (ascii.charCodeAt(i) & 0x3ff));
      bytes.push(
        0xf0 | (code >> 18),
        0x80 | ((code >> 12) & 0x3f),
        0x80 | ((code >> 6) & 0x3f),
        0x80 | (code & 0x3f)
      );
    }
  }

  const bitLength = bytes.length * 8;

  // Append 0x80
  bytes.push(0x80);

  // Pad with zeroes until length % 64 === 56
  while (bytes.length % 64 !== 56) {
    bytes.push(0);
  }

  // Append 64-bit length in big-endian
  bytes.push(0, 0, 0, 0);
  bytes.push(
    (bitLength >>> 24) & 0xff,
    (bitLength >>> 16) & 0xff,
    (bitLength >>> 8) & 0xff,
    bitLength & 0xff
  );

  let h0 = 0x6a09e667;
  let h1 = 0xbb67ae85;
  let h2 = 0x3c6ef372;
  let h3 = 0xa54ff53a;
  let h4 = 0x510e527f;
  let h5 = 0x9b05688c;
  let h6 = 0x1f83d9ab;
  let h7 = 0x5be0cd19;

  const w = new Uint32Array(64);

  for (let chunk = 0; chunk < bytes.length; chunk += 64) {
    for (let i = 0; i < 16; i++) {
      const idx = chunk + i * 4;
      w[i] =
        (bytes[idx] << 24) |
        (bytes[idx + 1] << 16) |
        (bytes[idx + 2] << 8) |
        bytes[idx + 3];
    }

    for (let i = 16; i < 64; i++) {
      const s0 = ror(w[i - 15], 7) ^ ror(w[i - 15], 18) ^ (w[i - 15] >>> 3);
      const s1 = ror(w[i - 2], 17) ^ ror(w[i - 2], 19) ^ (w[i - 2] >>> 10);
      w[i] = ((w[i - 16] + s0) | 0) + ((w[i - 7] + s1) | 0);
    }

    let a = h0;
    let b = h1;
    let c = h2;
    let d = h3;
    let e = h4;
    let f = h5;
    let g = h6;
    let h = h7;

    for (let i = 0; i < 64; i++) {
      const s1 = ror(e, 6) ^ ror(e, 11) ^ ror(e, 25);
      const ch = (e & f) ^ (~e & g);
      const temp1 = ((((h + s1) | 0) + ((ch + SHA256_K[i]) | 0)) | 0) + w[i];
      const s0 = ror(a, 2) ^ ror(a, 13) ^ ror(a, 22);
      const maj = (a & b) ^ (a & c) ^ (b & c);
      const temp2 = (s0 + maj) | 0;

      h = g;
      g = f;
      f = e;
      e = (d + temp1) | 0;
      d = c;
      c = b;
      b = a;
      a = (temp1 + temp2) | 0;
    }

    h0 = (h0 + a) | 0;
    h1 = (h1 + b) | 0;
    h2 = (h2 + c) | 0;
    h3 = (h3 + d) | 0;
    h4 = (h4 + e) | 0;
    h5 = (h5 + f) | 0;
    h6 = (h6 + g) | 0;
    h7 = (h7 + h) | 0;
  }

  const toHex = (n: number) => (n >>> 0).toString(16).padStart(8, '0');
  return `${toHex(h0)}${toHex(h1)}${toHex(h2)}${toHex(h3)}${toHex(h4)}${toHex(h5)}${toHex(h6)}${toHex(h7)}`;
}

/**
 * Universal SHA-256 calculation.
 * Uses native node:crypto when present in Node environments, falling back to pure TS on Hermes.
 */
export function sha256Hex(input: string): string {
  try {
    if (typeof process !== 'undefined' && process.versions && process.versions.node) {
      // eslint-disable-next-line @typescript-eslint/no-require-imports
      const nodeCrypto = require('crypto');
      if (nodeCrypto && typeof nodeCrypto.createHash === 'function') {
        return nodeCrypto.createHash('sha256').update(input, 'utf8').digest('hex');
      }
    }
  } catch {
    // Fall back to pure JS
  }
  return pureSha256(input);
}

// ============================================================================
// Canonical Admit Card Digest Computation
// ============================================================================

/**
 * Computes deterministic SHA-256 canonical hex digest of an Admit Card payload.
 */
export function computeCanonicalAdmitCardDigest(
  payload: Omit<SignedQrPayload, 'signature' | 'digest'> & { digest?: string }
): string {
  const canonicalParts = [
    payload.version || '1.0',
    payload.keyId || '',
    payload.algorithm || '',
    payload.hallTicketNumber || '',
    payload.candidateId || '',
    payload.candidateName || '',
    payload.examId || '',
    payload.shiftId || '',
    payload.centreId || '',
    String(payload.issuedAt ?? 0),
    String(payload.expiresAt ?? 0),
  ];

  return sha256Hex(canonicalParts.join(':'));
}

/**
 * Generates cryptographic digital signature string for testing or signing service.
 * Supports ECDSA/Ed25519 PEM keys in Node.js, and deterministic HMAC/mock keys offline.
 */
export function generateAdmitCardSignature(
  digest: string,
  privateKeyOrSecret: string,
  algorithm: 'ECDSA_P256' | 'ED25519' = 'ECDSA_P256'
): string {
  try {
    if (
      typeof process !== 'undefined' &&
      process.versions &&
      process.versions.node &&
      privateKeyOrSecret.includes('PRIVATE KEY')
    ) {
      // eslint-disable-next-line @typescript-eslint/no-require-imports
      const nodeCrypto = require('crypto');
      const signer = nodeCrypto.createSign(algorithm === 'ED25519' ? 'ed25519' : 'SHA256');
      signer.update(digest);
      signer.end();
      return signer.sign(privateKeyOrSecret, 'hex');
    }
  } catch {
    // Fallback to deterministic HMAC digest
  }

  // Deterministic HMAC-style signature for tests & offline simulation
  return `sig_${sha256Hex(`${digest}:${privateKeyOrSecret}:${algorithm}`)}`;
}

/**
 * Verifies a cryptographic signature against a public key or authority secret.
 */
export function verifySignature(
  algorithm: 'ECDSA_P256' | 'ED25519',
  digest: string,
  signature: string,
  publicKeyOrSecret: string
): boolean {
  if (!signature || !publicKeyOrSecret) {
    return false;
  }

  // 1. If public key is a PEM key and running in Node
  try {
    if (
      typeof process !== 'undefined' &&
      process.versions &&
      process.versions.node &&
      publicKeyOrSecret.includes('PUBLIC KEY')
    ) {
      // eslint-disable-next-line @typescript-eslint/no-require-imports
      const nodeCrypto = require('crypto');
      const verifier = nodeCrypto.createVerify(algorithm === 'ED25519' ? 'ed25519' : 'SHA256');
      verifier.update(digest);
      verifier.end();
      return verifier.verify(publicKeyOrSecret, signature, 'hex');
    }
  } catch {
    // Fall back to token verification
  }

  // 2. Deterministic HMAC / token signature check
  const expectedSig = `sig_${sha256Hex(`${digest}:${publicKeyOrSecret}:${algorithm}`)}`;
  if (signature === expectedSig) {
    return true;
  }

  // Also support direct hex HMAC match
  const rawExpectedHmac = sha256Hex(`${digest}:${publicKeyOrSecret}:${algorithm}`);
  return signature === rawExpectedHmac;
}

// ============================================================================
// Encoding, Parsing & Offline QR Verification
// ============================================================================

const HALL_TICKET_REGEX = /^HT-\d{4}-\d{6}$/;

/**
 * Encodes a Digital Admit Card into a compact, cryptographically signed JSON QR payload.
 */
export function encodeAdmitCardQrPayload(
  card: DigitalAdmitCard,
  signature: string,
  algorithm: 'ECDSA_P256' | 'ED25519' = 'ECDSA_P256',
  keyId = 'NTA-KMS-2026-PRIMARY'
): string {
  const issuedAt =
    typeof card.issuedAt === 'string' ? new Date(card.issuedAt).getTime() : card.issuedAt;
  const expiresAt =
    typeof card.expiresAt === 'string' ? new Date(card.expiresAt).getTime() : card.expiresAt;

  const unsigned: Omit<SignedQrPayload, 'signature' | 'digest'> = {
    version: '1.0',
    keyId,
    algorithm,
    hallTicketNumber: card.hallTicketNumber,
    candidateId: card.candidateId,
    candidateName: card.candidateName,
    examId: card.examId,
    shiftId: card.shiftId,
    centreId: card.centreId,
    issuedAt,
    expiresAt,
  };

  const digest = computeCanonicalAdmitCardDigest(unsigned);

  const payload: SignedQrPayload = {
    ...unsigned,
    digest,
    signature,
  };

  return JSON.stringify(payload);
}

/**
 * Parses raw QR code string data into a structured SignedQrPayload.
 * Returns null if data is malformed or missing essential cryptographic keys.
 */
export function parseAdmitCardQrPayload(rawQrData: string): SignedQrPayload | null {
  if (!rawQrData || typeof rawQrData !== 'string') {
    return null;
  }

  try {
    const trimmed = rawQrData.trim();
    const parsed = JSON.parse(trimmed) as Partial<SignedQrPayload>;

    if (
      !parsed.version ||
      !parsed.keyId ||
      !parsed.algorithm ||
      !parsed.hallTicketNumber ||
      !parsed.candidateId ||
      !parsed.examId ||
      !parsed.shiftId ||
      !parsed.centreId ||
      typeof parsed.issuedAt !== 'number' ||
      typeof parsed.expiresAt !== 'number' ||
      !parsed.digest ||
      !parsed.signature
    ) {
      return null;
    }

    return parsed as SignedQrPayload;
  } catch {
    return null;
  }
}

/**
 * Verifies an offline Admit Card / Hall Ticket QR payload.
 * Validates:
 * 1. JSON parsing & structural integrity
 * 2. Hall Ticket number format (^HT-\d{4}-\d{6}$)
 * 3. Expiration time check against currentTimestampMs
 * 4. Tamper-evident canonical SHA-256 digest match
 * 5. Digital signature verification against trusted authority public keys (if provided)
 */
export function verifyOfflineAdmitCardQr(
  rawQrData: string,
  trustedPublicKeys?: Record<string, string>,
  currentTimestampMs?: number
): QrVerificationResult {
  const checkedAt = currentTimestampMs ?? Date.now();

  // 1. Structural integrity check
  const payload = parseAdmitCardQrPayload(rawQrData);
  if (!payload) {
    return {
      valid: false,
      code: 'MALFORMED',
      message: 'Malformed QR payload: unable to parse JSON or mandatory cryptographic fields are missing',
      checkedAt,
    };
  }

  // 2. Hall Ticket number format check (^HT-\d{4}-\d{6}$)
  if (!HALL_TICKET_REGEX.test(payload.hallTicketNumber)) {
    return {
      valid: false,
      code: 'FORMAT_MISMATCH',
      message: `Invalid Hall Ticket format: ${payload.hallTicketNumber} does not match ^HT-\\d{4}-\\d{6}$`,
      payload,
      checkedAt,
    };
  }

  // 3. Expiration check
  if (payload.expiresAt && checkedAt > payload.expiresAt) {
    return {
      valid: false,
      code: 'EXPIRED',
      message: `Admit Card expired at timestamp ${payload.expiresAt} (current: ${checkedAt})`,
      payload,
      checkedAt,
    };
  }

  // 4. Tamper-evident canonical digest verification
  const computedDigest = computeCanonicalAdmitCardDigest(payload);
  if (computedDigest !== payload.digest) {
    return {
      valid: false,
      code: 'INVALID_DIGEST',
      message: `Tamper detected: Canonical digest mismatch (expected: ${computedDigest}, found: ${payload.digest})`,
      payload,
      checkedAt,
    };
  }

  // 5. Trusted Key & Signature verification
  if (trustedPublicKeys && Object.keys(trustedPublicKeys).length > 0) {
    const trustedKey = trustedPublicKeys[payload.keyId];
    if (!trustedKey) {
      return {
        valid: false,
        code: 'UNTRUSTED_KEY',
        message: `Authority Key ID ${payload.keyId} is not in the trusted public key store`,
        payload,
        checkedAt,
      };
    }

    const isSigValid = verifySignature(
      payload.algorithm,
      payload.digest,
      payload.signature,
      trustedKey
    );

    if (!isSigValid) {
      return {
        valid: false,
        code: 'INVALID_SIGNATURE',
        message: 'Cryptographic signature verification failed against authority public key',
        payload,
        checkedAt,
      };
    }
  }

  return {
    valid: true,
    code: 'VALID',
    message: 'Hall Ticket QR code is cryptographically valid and verified',
    payload,
    checkedAt,
  };
}
