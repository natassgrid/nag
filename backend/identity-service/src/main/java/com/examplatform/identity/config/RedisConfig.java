package com.examplatform.identity.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    /**
     * Customise the Lettuce client to disable auto-reconnect.
     *
     * By default Lettuce keeps a background reconnect thread alive. When the
     * service receives SIGTERM, graceful shutdown completes and the Docker
     * network is torn down, but Lettuce's reconnect thread is still mid-flight
     * and logs a WARN ("Cannot reconnect... Connection refused") that looks
     * alarming but is harmless. Disabling auto-reconnect eliminates that noise
     * because Lettuce will not attempt to re-establish a connection once the
     * existing one is lost during shutdown.
     */
    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration() {
        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(false)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build())
                .build();

        return LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
