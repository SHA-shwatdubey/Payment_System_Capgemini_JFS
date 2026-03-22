package com.wallet.notification.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTokenTest {

    @Test
    void gettersAndSetters_work() {
        DeviceToken token = new DeviceToken();
        token.setId(10L);
        token.setUserId(22L);
        token.setToken("abc-token");

        assertThat(token.getId()).isEqualTo(10L);
        assertThat(token.getUserId()).isEqualTo(22L);
        assertThat(token.getToken()).isEqualTo("abc-token");
    }
}

