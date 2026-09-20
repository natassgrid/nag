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

package com.examplatform.identity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {
    private boolean enabled = false;
    private boolean mobileVerificationRequired = false;
    private int weeklyLimit = 3;
    private int rollingWindowDays = 7;
    private Msg91Properties msg91 = new Msg91Properties();

    @Data
    public static class Msg91Properties {
        private String authKey;
        private String templateId;
        private String senderId = "NAGGov";
        private String apiUrl = "https://control.msg91.com/api/v5/otp";
    }
}
