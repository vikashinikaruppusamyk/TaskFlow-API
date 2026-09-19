package com.example.taskflow.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** The application must refuse to start with a missing or weak JWT secret. */
class JwtPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class)
            .withPropertyValues("taskflow.jwt.expiration=1h", "taskflow.jwt.issuer=taskflow-api");

    @Test
    void missingSecretStopsStartup() {
        runner.withPropertyValues("taskflow.jwt.secret=")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("JWT_SECRET"));
    }

    @Test
    void shortSecretStopsStartup() {
        runner.withPropertyValues("taskflow.jwt.secret=too-short")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("at least 32 characters"));
    }

    @Test
    void longEnoughSecretIsAccepted() {
        runner.withPropertyValues("taskflow.jwt.secret=a-secret-that-is-comfortably-over-32-characters")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtProperties.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class PropertiesConfig {
    }
}
