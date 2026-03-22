package com.wallet.admin.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class FeignAuthForwardingConfigTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void interceptor_forwardsAuthorizationAndRoleHeaders() {
        FeignAuthForwardingConfig config = new FeignAuthForwardingConfig();
        RequestInterceptor interceptor = config.authForwardingInterceptor();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-1");
        request.addHeader("X-Authenticated-Role", "ADMIN");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        Collection<String> auth = template.headers().get(HttpHeaders.AUTHORIZATION);
        Collection<String> role = template.headers().get("X-Authenticated-Role");
        assertThat(auth).contains("Bearer token-1");
        assertThat(role).contains("ADMIN");
    }
}

