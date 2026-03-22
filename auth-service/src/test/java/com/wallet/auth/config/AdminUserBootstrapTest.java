package com.wallet.auth.config;

import com.wallet.auth.entity.AuthUser;
import com.wallet.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserBootstrapTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void run_whenAdminMissing_createsAdminUser() {
        AdminUserBootstrap bootstrap = new AdminUserBootstrap(authUserRepository, passwordEncoder, "admin", "Admin@123");
        when(authUserRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin@123")).thenReturn("encoded");

        bootstrap.run();

        ArgumentCaptor<AuthUser> captor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authUserRepository).save(captor.capture());
        AuthUser saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void run_whenAdminAlreadyExists_doesNotCreateAgain() {
        AdminUserBootstrap bootstrap = new AdminUserBootstrap(authUserRepository, passwordEncoder, "admin", "Admin@123");
        when(authUserRepository.findByUsername("admin")).thenReturn(Optional.of(new AuthUser()));

        bootstrap.run();

        verify(authUserRepository, never()).save(any(AuthUser.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }
}


