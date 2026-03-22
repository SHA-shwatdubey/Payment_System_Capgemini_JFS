package com.wallet.auth;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.wallet.auth.entity.AuthUser;
import com.wallet.auth.repository.AuthUserRepository;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Auth Service API", version = "1.0"))
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDB(AuthUserRepository repository) {
        return args -> {
            if (repository.findByUsername("shashwat").isEmpty()) {
                repository.save(new AuthUser(null, "shashwat", "shashwat@123", "ADMIN"));
            }
        };
    }
}

