package com.examplatform.delivery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OfflineDeliveryService.
 * Validates: Requirements 9.7
 */
@ExtendWith(MockitoExtension.class)
class OfflineDeliveryServiceTest {

    @Mock
    private VaultCryptoService vaultCryptoService;

    private OfflineDeliveryService offlineDeliveryService;

    @BeforeEach
    void setUp() {
        offlineDeliveryService = new OfflineDeliveryService(vaultCryptoService);
    }

    @Test
    @DisplayName("preloadExamPackage decrypts using center-specific time-limited key")
    void preloadExamPackage_decryptsCorrectly() {
        UUID sessionId = UUID.randomUUID();
        String centerId = "CENTER-001";
        String tenantId = "tenant-abc";

        String dateStamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expectedKeyName = "center-key-" + centerId + "-" + dateStamp;
        String expectedDecrypted = "{\"questions\":[{\"id\":\"q1\",\"content\":\"What is 2+2?\"}]}";

        when(vaultCryptoService.decrypt(eq(expectedKeyName), anyString()))
                .thenReturn(expectedDecrypted);

        String result = offlineDeliveryService.preloadExamPackage(sessionId, centerId, tenantId);

        assertThat(result).isEqualTo(expectedDecrypted);
        verify(vaultCryptoService).decrypt(eq(expectedKeyName), anyString());
    }

    @Test
    @DisplayName("preloadExamPackage uses correct key naming convention with date stamp")
    void preloadExamPackage_usesCorrectKeyNaming() {
        UUID sessionId = UUID.randomUUID();
        String centerId = "CENTER-XYZ";
        String tenantId = "tenant-123";

        String dateStamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expectedKeyName = "center-key-CENTER-XYZ-" + dateStamp;

        when(vaultCryptoService.decrypt(eq(expectedKeyName), anyString()))
                .thenReturn("decrypted-content");

        offlineDeliveryService.preloadExamPackage(sessionId, centerId, tenantId);

        verify(vaultCryptoService).decrypt(eq(expectedKeyName), anyString());
    }

    @Test
    @DisplayName("reconcileOnReconnect completes without error")
    void reconcileOnReconnect_completesSuccessfully() {
        UUID sessionId = UUID.randomUUID();
        String tenantId = "tenant-abc";

        // Should complete without throwing any exception
        offlineDeliveryService.reconcileOnReconnect(sessionId, tenantId);
    }
}
