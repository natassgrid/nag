package com.examplatform.translation.service;

/**
 * Contract for HSM/Vault-backed cryptographic operations.
 * Used for encrypting translation content at rest via Vault Transit.
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
     *
     * @param keyName    name of the Transit key to use
     * @param ciphertext Vault ciphertext blob
     * @return original plaintext
     */
    String decrypt(String keyName, String ciphertext);
}
