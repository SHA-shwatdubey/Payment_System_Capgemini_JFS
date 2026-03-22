package com.wallet.rewards;

import com.wallet.rewards.repository.RewardCatalogItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardsServiceApplicationTest {

    @Test
    void applicationClass_hasSpringBootAnnotation() {
        assertThat(RewardsServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = RewardsServiceApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }

    @Test
    void initDb_savesSeedItemWhenCatalogEmpty() throws Exception {
        RewardsServiceApplication app = new RewardsServiceApplication();
        RewardCatalogItemRepository repository = mock(RewardCatalogItemRepository.class);
        when(repository.count()).thenReturn(0L);

        CommandLineRunner runner = app.initDB(repository);
        runner.run();

        verify(repository).save(any());
    }

    @Test
    void initDb_skipsSeedWhenCatalogHasData() throws Exception {
        RewardsServiceApplication app = new RewardsServiceApplication();
        RewardCatalogItemRepository repository = mock(RewardCatalogItemRepository.class);
        when(repository.count()).thenReturn(5L);

        CommandLineRunner runner = app.initDB(repository);
        runner.run();

        verify(repository, never()).save(any());
    }
}

