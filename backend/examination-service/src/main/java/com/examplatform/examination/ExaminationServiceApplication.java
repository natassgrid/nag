package com.examplatform.examination;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExaminationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExaminationServiceApplication.class, args);
    }
}
