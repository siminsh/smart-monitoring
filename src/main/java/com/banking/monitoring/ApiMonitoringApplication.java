package com.banking.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiMonitoringApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiMonitoringApplication.class, args);
    }
} 