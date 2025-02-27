package com.festago.support;

import com.festago.auth.application.AuthExtractor;
import com.festago.presentation.auth.AuthenticateContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestAuthConfig {

    @Bean
    public AuthenticateContext authenticateContext() {
        return new AuthenticateContext();
    }

    @Bean
    public AuthExtractor authExtractor(AuthenticateContext authenticateContext) {
        return new MockAuthExtractor(authenticateContext);
    }
}
