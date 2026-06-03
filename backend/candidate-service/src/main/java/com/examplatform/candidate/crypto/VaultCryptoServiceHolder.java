package com.examplatform.candidate.crypto;

import com.examplatform.candidate.service.VaultCryptoService;
import org.springframework.stereotype.Component;

/**
 * Static holder for {@link VaultCryptoService} to make it accessible
 * from JPA {@link jakarta.persistence.AttributeConverter} instances,
 * which are not Spring-managed beans by default.
 */
@Component
public class VaultCryptoServiceHolder {

    private static VaultCryptoService instance;

    public VaultCryptoServiceHolder(VaultCryptoService service) {
        VaultCryptoServiceHolder.instance = service;
    }

    public static VaultCryptoService getInstance() {
        return instance;
    }
}
