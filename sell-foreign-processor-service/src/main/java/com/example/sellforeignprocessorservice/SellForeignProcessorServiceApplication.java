package com.example.sellforeignprocessorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SellForeignProcessorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SellForeignProcessorServiceApplication.class, args);
    }

}
