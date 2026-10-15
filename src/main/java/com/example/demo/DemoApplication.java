package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @GetMapping("/api/environment")
    public Map<String, Object> getEnvironment() {
        Map<String, Object> response = new HashMap<>();
        response.put("environment", System.getenv().getOrDefault("DEPLOY_ENV", "local"));
        response.put("status", "ACTIVE");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
