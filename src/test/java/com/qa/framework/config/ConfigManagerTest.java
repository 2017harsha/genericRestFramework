package com.qa.framework.config;

import com.qa.framework.secrets.SecretResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Offline self-tests of the framework core (no network, no AWS). Run with -Dgroups=framework. */
@Tag("framework")
@Isolated
class ConfigManagerTest {

    private final String originalEnv = System.getProperty("env");

    @AfterEach
    void restore() {
        if (originalEnv == null) {
            System.clearProperty("env");
        } else {
            System.setProperty("env", originalEnv);
        }
        System.clearProperty("base.url");
        ConfigManager.reset();
    }

    @Test
    void loadsEnvironmentFileSelectedBySystemProperty() {
        System.setProperty("env", "dev");
        ConfigManager.reset();

        ConfigManager cfg = ConfigManager.get();
        assertThat(cfg.environment()).isEqualTo(Environment.DEV);
        assertThat(cfg.get("log.mode")).isEqualTo("all");          // from dev.properties
        assertThat(cfg.get("http.read.timeout.ms")).isEqualTo("30000"); // from common.properties
    }

    @Test
    void systemPropertyOverridesFile() {
        System.setProperty("env", "qa");
        System.setProperty("base.url", "https://override.example.com");
        ConfigManager.reset();

        assertThat(ConfigManager.get().get("base.url")).isEqualTo("https://override.example.com");
    }

    @Test
    void prodPasswordIsAnSsmReferenceAndNotResolvedEagerly() {
        System.setProperty("env", "prod");
        ConfigManager.reset();

        ConfigManager cfg = ConfigManager.get();
        assertThat(cfg.isSecretReference("auth.password")).isTrue();
        assertThat(cfg.raw("auth.password")).contains("ssm:/api-test-framework/prod/api-password");
    }

    @Test
    void envSecretReferenceIsResolved() {
        System.setProperty("FRAMEWORK_SELFTEST_SECRET", "s3cr3t");
        try {
            assertThat(SecretResolver.resolve("env:FRAMEWORK_SELFTEST_SECRET")).isEqualTo("s3cr3t");
            assertThat(SecretResolver.resolve("plain-value")).isEqualTo("plain-value");
        } finally {
            System.clearProperty("FRAMEWORK_SELFTEST_SECRET");
        }
    }

    @Test
    void unknownEnvironmentFailsFast() {
        assertThatThrownBy(() -> Environment.from("staging"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("staging");
    }
}
