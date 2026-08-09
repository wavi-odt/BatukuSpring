package org.example.batuku;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BatukuApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatukuApplication.class, args);
    }
}
