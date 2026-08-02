package com.aakash.tradenest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradenestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradenestApplication.class, args);
    }
}