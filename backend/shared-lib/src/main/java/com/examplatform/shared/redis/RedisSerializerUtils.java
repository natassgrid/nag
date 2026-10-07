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

package com.examplatform.shared.redis;

import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.DeserializationFeature;

/**
 * Utility for creating pre-configured Redis JSON serializers with polymorphic typing support
 * across all microservices.
 *
 * <p>Uses the Spring Data Redis 4.x {@link GenericJacksonJsonRedisSerializer} (Jackson 3) builder
 * API, replacing the removed {@code GenericJackson2JsonRedisSerializer}.
 *
 * <p>Default typing embeds the Java class name as {@code @class} property in JSON, enabling
 * polymorphic deserialization. Only use this serializer for data stored in trusted internal Redis
 * instances — not for data originating from untrusted external sources.
 */
public final class RedisSerializerUtils {

    private RedisSerializerUtils() {}

    /**
     * Creates a {@link RedisSerializer} configured with unknown-property tolerance and default
     * typing (class name embedded as {@code @class}) for Redis caching.
     *
     * @return a configured {@link GenericJacksonJsonRedisSerializer}
     */
    public static RedisSerializer<Object> jsonSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .customize(b -> b.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                .build();
    }
}
