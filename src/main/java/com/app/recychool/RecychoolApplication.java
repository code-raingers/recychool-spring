package com.app.recychool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecychoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecychoolApplication.class, args);
    }

}
