package com.wallet.auth;

import com.wallet.auth.entity.AuthUser;
import com.wallet.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceApplicationTest {

    @Test
    void initDB_whenDefaultAdminMissing_savesAdminUser() throws Exception {
        AuthServiceApplication app = new AuthServiceApplication();
        AuthUserRepository repository = mock(AuthUserRepository.class);
        when(repository.findByUsername("shashwat")).thenReturn(Optional.empty());

        CommandLineRunner runner = app.initDB(repository);
        runner.run();

        ArgumentCaptor<AuthUser> captor = ArgumentCaptor.forClass(AuthUser.class);
        verify(repository).save(captor.capture());
        AuthUser saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("shashwat");
        assertThat(saved.getPassword()).isEqualTo("shashwat@123");
        assertThat(saved.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void initDB_whenDefaultAdminExists_doesNotSaveAgain() throws Exception {
        AuthServiceApplication app = new AuthServiceApplication();
        AuthUserRepository repository = mock(AuthUserRepository.class);
        when(repository.findByUsername("shashwat")).thenReturn(Optional.of(new AuthUser()));

        CommandLineRunner runner = app.initDB(repository);
        runner.run();

        verify(repository, never()).save(any(AuthUser.class));
    }
}

