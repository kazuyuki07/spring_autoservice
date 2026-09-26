package com.pashinin.autoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AutoserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoserviceApplication.class, args);
    }

}
