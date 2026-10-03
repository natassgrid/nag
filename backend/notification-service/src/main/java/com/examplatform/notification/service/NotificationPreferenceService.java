/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.notification.service;

import com.examplatform.notification.domain.NotificationPreference;
import com.examplatform.notification.dto.NotificationPreferenceRequest;
import com.examplatform.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service to manage candidate communication channel preferences.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Gets user communication preferences, creating default preferences if none exist.
     */
    @Transactional(readOnly = true)
    public NotificationPreference getPreferences(UUID userId, String tenantId) {
        return preferenceRepository.findByUserIdAndTenantId(userId, tenantId)
                .or(() -> preferenceRepository.findByUserId(userId))
                .orElseGet(() -> {
                    NotificationPreference defaultPref = NotificationPreference.builder()
                            .userId(userId)
                            .preferredChannel("EMAIL")
                            .pushEnabled(true)
                            .smsEnabled(true)
                            .whatsappEnabled(true)
                            .emailEnabled(true)
                            .inAppEnabled(true)
                            .build();
                    defaultPref.setTenantId(tenantId);
                    return defaultPref;
                });
    }

    /**
     * Updates or creates notification preferences for a user.
     */
    public NotificationPreference updatePreferences(UUID userId, String tenantId, NotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findByUserIdAndTenantId(userId, tenantId)
                .or(() -> preferenceRepository.findByUserId(userId))
                .orElseGet(() -> {
                    NotificationPreference np = NotificationPreference.builder()
                            .userId(userId)
                            .build();
                    np.setTenantId(tenantId);
                    return np;
                });

        if (request.getPreferredChannel() != null) {
            preference.setPreferredChannel(request.getPreferredChannel().toUpperCase());
        }
        if (request.getPhoneNumber() != null) {
            preference.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            preference.setEmail(request.getEmail());
        }
        if (request.getFcmToken() != null) {
            preference.setFcmToken(request.getFcmToken());
        }
        if (request.getPushEnabled() != null) {
            preference.setPushEnabled(request.getPushEnabled());
        }
        if (request.getSmsEnabled() != null) {
            preference.setSmsEnabled(request.getSmsEnabled());
        }
        if (request.getWhatsappEnabled() != null) {
            preference.setWhatsappEnabled(request.getWhatsappEnabled());
        }
        if (request.getEmailEnabled() != null) {
            preference.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getInAppEnabled() != null) {
            preference.setInAppEnabled(request.getInAppEnabled());
        }

        NotificationPreference saved = preferenceRepository.save(preference);
        log.info("Saved notification preferences for userId={}, preferredChannel={}",
                userId, saved.getPreferredChannel());
        return saved;
    }
}
