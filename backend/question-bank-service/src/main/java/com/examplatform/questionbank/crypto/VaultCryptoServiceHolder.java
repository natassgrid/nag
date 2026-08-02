package com.examplatform.questionbank.crypto;

import com.examplatform.questionbank.service.VaultCryptoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Static holder for {@link VaultCryptoService} and the encryption feature flag.
 * Bridges Spring-managed beans into JPA {@link jakarta.persistence.AttributeConverter}
 * instances, which are not Spring-managed by default.
 */
@Component
public class VaultCryptoServiceHolder {

    private static VaultCryptoService instance;
    private static boolean encryptionEnabled;

    public VaultCryptoServiceHolder(
            VaultCryptoService service,
            @Value("${app.encryption.enabled:false}") boolean encryptionEnabled) {
        VaultCryptoServiceHolder.instance = service;
        VaultCryptoServiceHolder.encryptionEnabled = encryptionEnabled;
    }

    public static VaultCryptoService getInstance() {
        return instance;
    }

    public static boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
}
