// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration.class })
@EnableJpaRepositories(basePackages = "com.examplatform.practice.repository")
@EnableScheduling
public class PracticeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PracticeServiceApplication.class, args);
    }
}
