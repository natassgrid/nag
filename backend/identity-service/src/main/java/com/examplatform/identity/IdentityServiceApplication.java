package com.examplatform.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Exclude RedisRepositoriesAutoConfiguration because Redis is used only for
// Spring Session (via RedisTemplate), not for Spring Data Redis repositories.
// Without this exclusion, Spring Data sees both JPA and Redis on the classpath
// and enters "strict repository configuration mode", scanning every repository
// interface against both stores — adding ~60-70 seconds to startup time.
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@EnableJpaRepositories(basePackages = "com.examplatform.identity.repository")
@EnableAsync
@EnableScheduling
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
