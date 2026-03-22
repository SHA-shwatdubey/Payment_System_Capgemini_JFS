package com.wallet.userkyc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@OpenAPIDefinition(info = @Info(title = "User KYC Service API", version = "1.0"))
public class UserKycServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserKycServiceApplication.class, args);
    }
}

