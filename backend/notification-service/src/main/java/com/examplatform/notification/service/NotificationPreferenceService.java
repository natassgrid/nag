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

import java.util.UUID;
import java.util.function.Consumer;

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
        setIfPresent(request.getPhoneNumber(), preference::setPhoneNumber);
        setIfPresent(request.getEmail(), preference::setEmail);
        setIfPresent(request.getFcmToken(), preference::setFcmToken);
        setIfPresent(request.getPushEnabled(), preference::setPushEnabled);
        setIfPresent(request.getSmsEnabled(), preference::setSmsEnabled);
        setIfPresent(request.getWhatsappEnabled(), preference::setWhatsappEnabled);
        setIfPresent(request.getEmailEnabled(), preference::setEmailEnabled);
        setIfPresent(request.getInAppEnabled(), preference::setInAppEnabled);

        NotificationPreference saved = preferenceRepository.save(preference);
        log.info("Saved notification preferences for userId={}, preferredChannel={}",
                userId, saved.getPreferredChannel());
        return saved;
    }

    private static <T> void setIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
