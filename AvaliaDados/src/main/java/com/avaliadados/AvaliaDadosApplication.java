package com.avaliadados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AvaliaDadosApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvaliaDadosApplication.class, args);
    }

}
