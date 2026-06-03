package com.examplatform.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;
import org.springframework.vault.support.Signature;

import java.time.Instant;
import java.util.Map;

/**
 * HSM/Vault-backed implementation of {@link VaultCryptoService}.
 * Delegates all cryptographic operations to Vault Transit, ensuring that
 * private key material never leaves the Vault boundary.
 *
 * Primary use case in the delivery service: shift-key decryption to unlock
 * encrypted question papers at the scheduled exam start time.
 *
 * Validates: Requirements 9.1, 19.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Primary
public class VaultCryptoServiceImpl implements VaultCryptoService {

    private final VaultTemplate vaultTemplate;

    private VaultTransitOperations transit() {
        return vaultTemplate.opsForTransit();
    }

    @Override
    public String encrypt(String keyName, String plaintext) {
        try {
            Ciphertext ciphertext = transit().encrypt(keyName, Plaintext.of(plaintext));
            return ciphertext.getCiphertext();
        } catch (Exception e) {
            log.error("Vault encrypt failed for key [{}]: {}", keyName, e.getMessage());
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String decrypt(String keyName, String ciphertext) {
        try {
            Plaintext plaintext = transit().decrypt(keyName, Ciphertext.of(ciphertext));
            return plaintext.asString();
        } catch (Exception e) {
            log.error("Vault decrypt failed for key [{}]: {}", keyName, e.getMessage());
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String sign(String keyName, String payload) {
        try {
            Plaintext input = Plaintext.of(payload);
            return transit().sign(keyName, input).getSignature();
        } catch (Exception e) {
            log.error("Vault sign failed for key [{}]: {}", keyName, e.getMessage());
            throw new RuntimeException("Signing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verify(String keyName, String payload, String signature) {
        try {
            Plaintext input = Plaintext.of(payload);
            return transit().verify(keyName, input, Signature.of(signature));
        } catch (Exception e) {
            log.error("Vault verify failed for key [{}]: {}", keyName, e.getMessage());
            return false;
        }
    }

    @Override
    public void rotateKey(String keyName) {
        try {
            transit().rotate(keyName);
            log.info("Vault Transit key [{}] rotated successfully", keyName);
        } catch (Exception e) {
            log.error("Vault key rotation failed for key [{}]: {}", keyName, e.getMessage());
            throw new RuntimeException("Key rotation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void revokeKey(String keyName) {
        try {
            // First allow deletion, then delete the key
            vaultTemplate.write("transit/keys/" + keyName + "/config",
                    Map.of("deletion_allowed", true));
            vaultTemplate.delete("transit/keys/" + keyName);
            log.warn("SECURITY: Vault Transit key [{}] revoked (deleted) at {}", keyName, Instant.now());
        } catch (Exception e) {
            log.error("Vault key revocation failed for key [{}]: {}", keyName, e.getMessage());
            throw new RuntimeException("Key revocation failed: " + e.getMessage(), e);
        }
    }
}
