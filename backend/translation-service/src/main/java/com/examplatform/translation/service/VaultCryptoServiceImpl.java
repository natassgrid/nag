package com.examplatform.translation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;

/**
 * Vault Transit-backed implementation of {@link VaultCryptoService}.
 * Encrypts/decrypts translation content; key material never leaves Vault.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VaultCryptoServiceImpl implements VaultCryptoService {

    private final VaultTemplate vaultTemplate;

    private VaultTransitOperations transit() {
        return vaultTemplate.opsForTransit();
    }

    @Override
    public String encrypt(String keyName, String plaintext) {
        try {
            return transit().encrypt(keyName, Plaintext.of(plaintext)).getCiphertext();
        } catch (Exception e) {
            log.error("Vault encrypt failed for key [{}]: {}", keyName, e.getMessage());
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String decrypt(String keyName, String ciphertext) {
        try {
            return transit().decrypt(keyName, Ciphertext.of(ciphertext)).asString();
        } catch (Exception e) {
            log.error("Vault decrypt failed for key [{}]: {}", keyName, e.getMessage());
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }
}
