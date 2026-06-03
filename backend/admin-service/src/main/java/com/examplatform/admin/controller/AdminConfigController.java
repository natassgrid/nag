package com.examplatform.admin.controller;

import com.examplatform.admin.domain.SystemConfig;
import com.examplatform.admin.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller stub for system configuration management.
 * Accessible only by SUPER_ADMIN and SECURITY_ADMIN roles.
 */
@RestController
@RequestMapping("/api/v1/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')")
public class AdminConfigController {

    private final SystemConfigRepository systemConfigRepository;

    /**
     * Retrieves all system configuration parameters for the given tenant.
     */
    @GetMapping
    public ResponseEntity<List<SystemConfig>> getConfig(@RequestParam String tenantId) {
        List<SystemConfig> configs = systemConfigRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(configs);
    }

    /**
     * Updates a system configuration parameter.
     */
    @PutMapping
    public ResponseEntity<SystemConfig> updateConfig(@RequestBody SystemConfig config) {
        // TODO: Validate input, set updatedBy from JWT principal, persist
        SystemConfig saved = systemConfigRepository.save(config);
        return ResponseEntity.ok(saved);
    }
}
