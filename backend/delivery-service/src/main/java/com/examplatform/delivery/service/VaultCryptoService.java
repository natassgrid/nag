package com.examplatform.delivery.service;

/**
 * Contract for HSM/Vault-backed cryptographic operations.
 * Used in the delivery service for shift-key decryption — ensuring that
 * private key material never leaves the Vault boundary.
 */
public interface VaultCryptoService {

    /**
     * Encrypt the given plaintext using the named Vault Transit key.
     *
     * @param keyName   name of the Transit key to use
     * @param plaintext data to encrypt
     * @return Vault ciphertext blob (vault:v1:...)
     */
    String encrypt(String keyName, String plaintext);

    /**
     * Decrypt the given Vault ciphertext using the named Transit key.
     * Primary use case: decrypting shift keys for exam paper delivery.
     *
     * @param keyName    name of the Transit key to use
     * @param ciphertext Vault ciphertext blob
     * @return original plaintext
     */
    String decrypt(String keyName, String ciphertext);

    /**
     * Sign the payload using the named Vault Transit key (ECDSA/HMAC).
     *
     * @param keyName name of the signing key
     * @param payload data to sign
     * @return Base64-encoded signature
     */
    String sign(String keyName, String payload);

    /**
     * Verify a previously produced signature against the payload.
     *
     * @param keyName   name of the signing key
     * @param payload   original data
     * @param signature signature to verify
     * @return {@code true} if the signature is valid, {@code false} otherwise
     */
    boolean verify(String keyName, String payload, String signature);

    /**
     * Rotate the named Transit key to a new version.
     * Existing ciphertexts remain decryptable with the previous key version.
     *
     * @param keyName name of the key to rotate
     */
    void rotateKey(String keyName);

    /**
     * Revoke the named Transit key, rendering all associated ciphertexts
     * permanently unreadable.
     *
     * @param keyName name of the key to revoke
     */
    void revokeKey(String keyName);
}
