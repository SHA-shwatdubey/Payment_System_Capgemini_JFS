package com.wallet.auth.config;

import com.wallet.auth.entity.AuthUser;
import com.wallet.auth.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserBootstrap implements CommandLineRunner {

	private final AuthUserRepository authUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final String adminUsername;
	private final String adminPassword;

	public AdminUserBootstrap(AuthUserRepository authUserRepository,
							  PasswordEncoder passwordEncoder,
							  @Value("${security.bootstrap-admin.username:admin}") String adminUsername,
							  @Value("${security.bootstrap-admin.password:Admin@123}") String adminPassword) {
		this.authUserRepository = authUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
	}

	@Override
	public void run(String... args) {
		authUserRepository.findByUsername(adminUsername).orElseGet(() -> {
			AuthUser admin = new AuthUser();
			admin.setUsername(adminUsername);
			admin.setPassword(passwordEncoder.encode(adminPassword));
			admin.setRole("ADMIN");
			return authUserRepository.save(admin);
		});
	}
}

