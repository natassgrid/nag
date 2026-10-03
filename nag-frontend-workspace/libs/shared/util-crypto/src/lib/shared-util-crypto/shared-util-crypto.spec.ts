import {
  hashSha256,
  hashSha512,
  generateClientSalt,
  generateDeviceFingerprint,
  encryptPayloadWebCrypto,
  decryptPayloadWebCrypto,
  signSubmissionHash,
  computeMerkleLeafHash,
  combineMerkleNodes,
  verifyMerkleProof,
  MerkleProofStep,
} from './shared-util-crypto';

describe('shared-util-crypto', () => {
  it('should compute valid sha256 hash', async () => {
    const hash = await hashSha256('test-string');
    expect(hash).toBeDefined();
    expect(hash.length).toBe(64);
  });

  it('should compute valid sha512 hash', async () => {
    const hash = await hashSha512('test-string');
    expect(hash).toBeDefined();
    expect(hash.length).toBe(128);
  });

  it('should generate client salt of expected length', () => {
    const salt16 = generateClientSalt(16);
    expect(salt16).toBeDefined();
    expect(salt16.length).toBe(32);

    const saltDefault = generateClientSalt();
    expect(saltDefault.length).toBe(32);
  });

  it('should correctly compute and verify binary Merkle tree proofs', async () => {
    const leaf0 = await computeMerkleLeafHash('Q0: Question 1');
    const leaf1 = await computeMerkleLeafHash('Q1: Question 2');
    const leaf2 = await computeMerkleLeafHash('Q2: Question 3');
    const leaf3 = await computeMerkleLeafHash('Q3: Question 4');

    const node01 = await combineMerkleNodes(leaf0, leaf1);
    const node23 = await combineMerkleNodes(leaf2, leaf3);
    const root = await combineMerkleNodes(node01, node23);

    // Proof for leaf0: sibling leaf1 (RIGHT), sibling node23 (RIGHT)
    const proofLeaf0: MerkleProofStep[] = [
      { hash: leaf1, position: 'RIGHT' },
      { hash: node23, position: 'RIGHT' },
    ];

    const isLeaf0Valid = await verifyMerkleProof(leaf0, proofLeaf0, root);
    expect(isLeaf0Valid).toBe(true);

    // Proof for leaf2: sibling leaf3 (RIGHT), sibling node01 (LEFT)
    const proofLeaf2: MerkleProofStep[] = [
      { hash: leaf3, position: 'RIGHT' },
      { hash: node01, position: 'LEFT' },
    ];

    const isLeaf2Valid = await verifyMerkleProof(leaf2, proofLeaf2, root);
    expect(isLeaf2Valid).toBe(true);

    // Tampered leaf verification should fail
    const tamperedLeaf = await computeMerkleLeafHash('Q0: Tampered Question');
    const isTamperedValid = await verifyMerkleProof(tamperedLeaf, proofLeaf0, root);
    expect(isTamperedValid).toBe(false);
  });

  it('should generate device fingerprint with default and overridden navigator and screen', async () => {
    const fp1 = await generateDeviceFingerprint();
    expect(fp1).toBeDefined();
    expect(fp1.length).toBe(64);

    const origNavigator = globalThis.navigator;
    const origScreen = globalThis.screen;
    const origIntl = globalThis.Intl;
    try {
      Object.defineProperty(globalThis, 'navigator', {
        value: {
          userAgent: 'custom-agent',
          language: 'en-US',
          hardwareConcurrency: 8,
        },
        configurable: true,
      });
      Object.defineProperty(globalThis, 'screen', {
        value: { width: 1920, height: 1080, colorDepth: 24 },
        configurable: true,
      });
      const fp2 = await generateDeviceFingerprint();
      expect(fp2).toBeDefined();

      Object.defineProperty(globalThis, 'navigator', {
        value: { userAgent: '', language: '', hardwareConcurrency: '' },
        configurable: true,
      });
      Object.defineProperty(globalThis, 'screen', {
        value: { width: 0, height: 0, colorDepth: '' },
        configurable: true,
      });
      Object.defineProperty(globalThis, 'Intl', {
        value: { DateTimeFormat: () => ({ resolvedOptions: () => ({ timeZone: '' }) }) },
        configurable: true,
      });
      const fp3 = await generateDeviceFingerprint();
      expect(fp3).toBeDefined();
    } finally {
      Object.defineProperty(globalThis, 'navigator', {
        value: origNavigator,
        configurable: true,
      });
      Object.defineProperty(globalThis, 'screen', {
        value: origScreen,
        configurable: true,
      });
      Object.defineProperty(globalThis, 'Intl', {
        value: origIntl,
        configurable: true,
      });
    }
  });

  it('should encrypt and decrypt payload with AES-GCM', async () => {
    const rawKeyHex = '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';
    const payload = JSON.stringify({ student: 'John Doe', score: 98 });

    const encrypted = await encryptPayloadWebCrypto(payload, rawKeyHex);
    expect(encrypted.cipherText).toBeDefined();
    expect(encrypted.iv).toBeDefined();

    const decrypted = await decryptPayloadWebCrypto(
      encrypted.cipherText,
      encrypted.iv,
      rawKeyHex
    );
    expect(decrypted).toBe(payload);
    const parsed = JSON.parse(decrypted);
    expect(parsed.score).toBe(98);
  });

  it('should sign submission hash with and without full metadata', async () => {
    const sig1 = await signSubmissionHash('cand-123');
    expect(sig1).toBeDefined();
    expect(sig1.length).toBe(64);

    const sig2 = await signSubmissionHash(
      'cand-123',
      'exam-456',
      '{"q1":"A"}',
      '2026-09-27T10:00:00Z'
    );
    expect(sig2).toBeDefined();
    expect(sig2.length).toBe(64);
    expect(sig2).not.toEqual(sig1);

    const sig3 = await signSubmissionHash('cand-123', 'exam-456');
    expect(sig3).toBeDefined();
    expect(sig3.length).toBe(64);

    const sig4 = await signSubmissionHash('cand-123', 'exam-456', '{"q1":"A"}');
    expect(sig4).toBeDefined();
  });
});
