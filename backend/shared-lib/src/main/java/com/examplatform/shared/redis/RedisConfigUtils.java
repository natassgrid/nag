/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Shared utility for creating pre-configured Redis templates and Lettuce client configurations
 * across microservices.
 */
public final class RedisConfigUtils {

    private RedisConfigUtils() {}

    /**
     * Builds and initializes a standardized {@link RedisTemplate} with String key/hash-key serializers
     * and the shared JSON serializer for values.
     *
     * @param connectionFactory the Redis connection factory
     * @return initialized RedisTemplate
     */
    public static RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory connectionFactory) {
        return createRedisTemplate(connectionFactory, RedisSerializerUtils.jsonSerializer());
    }

    /**
     * Builds and initializes a standardized {@link RedisTemplate} with String key/hash-key serializers
     * and a custom value serializer.
     *
     * @param connectionFactory the Redis connection factory
     * @param valueSerializer serializer for values and hash values
     * @return initialized RedisTemplate
     */
    public static RedisTemplate<String, Object> createRedisTemplate(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<Object> valueSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Creates a standard {@link LettuceClientConfiguration} with disabled auto-reconnect,
     * standard connect timeout, and command timeout to prevent noisy reconnect logs during graceful shutdown.
     *
     * @param connectTimeout connection timeout duration
     * @param commandTimeout command timeout duration
     * @return configured LettuceClientConfiguration
     */
    public static LettuceClientConfiguration createLettuceClientConfiguration(
            Duration connectTimeout,
            Duration commandTimeout) {
        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(false)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(connectTimeout)
                        .build())
                .build();

        return LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(commandTimeout)
                .build();
    }

    /**
     * Creates a standard {@link LettuceClientConfiguration} with default 5-second connect timeout
     * and 2-second command timeout.
     *
     * @return configured LettuceClientConfiguration
     */
    public static LettuceClientConfiguration createDefaultLettuceClientConfiguration() {
        return createLettuceClientConfiguration(Duration.ofSeconds(5), Duration.ofSeconds(2));
    }
}
