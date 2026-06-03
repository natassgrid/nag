package com.examplatform.questionbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QuestionBankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuestionBankServiceApplication.class, args);
    }
}
