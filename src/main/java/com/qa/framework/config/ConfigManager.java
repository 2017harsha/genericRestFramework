package com.qa.framework.config;

import com.qa.framework.secrets.SecretResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Single source of configuration for the framework.
 *
 * <p>Lookup order for every key (first hit wins):</p>
 * <ol>
 *     <li>JVM system property, e.g. {@code -Dbase.url=https://...}</li>
 *     <li>OS environment variable, key upper-cased with dots → underscores, e.g. {@code BASE_URL}</li>
 *     <li>{@code config/<env>.properties} on the classpath</li>
 *     <li>{@code config/common.properties} on the classpath</li>
 * </ol>
 *
 * <p>Values may be <b>secret references</b> that are resolved lazily on read:</p>
 * <ul>
 *     <li>{@code ssm:/path/to/param} – AWS SSM Parameter Store (SecureString is decrypted)</li>
 *     <li>{@code env:VAR_NAME} – an OS environment variable (e.g. injected by Jenkins credentials)</li>
 * </ul>
 */
public final class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);
    private static volatile ConfigManager instance;

    private final Environment environment;
    private final Properties properties = new Properties();

    private ConfigManager() {
        String envValue = Optional.ofNullable(System.getProperty("env"))
                .filter(s -> !s.isBlank())
                .orElse(System.getenv("ENV"));
        this.environment = Environment.from(envValue);

        load("config/common.properties", false);
        load("config/" + environment.id() + ".properties", true);
        LOG.info("Loaded configuration for environment [{}]", environment.id());
    }

    public static ConfigManager get() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    /** For unit tests of the framework itself. */
    static synchronized void reset() {
        instance = null;
    }

    private void load(String resource, boolean required) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("Missing configuration file on classpath: " + resource);
                }
                return;
            }
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
    }

    public Environment environment() {
        return environment;
    }

    /** Raw value without secret resolution – safe for logging/reporting. */
    public Optional<String> raw(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return Optional.of(sys.trim());
        }
        String envVar = System.getenv(key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_'));
        if (envVar != null && !envVar.isBlank()) {
            return Optional.of(envVar.trim());
        }
        return Optional.ofNullable(properties.getProperty(key)).map(String::trim).filter(s -> !s.isEmpty());
    }

    /** Value with secret references ({@code ssm:}, {@code env:}) resolved. */
    public Optional<String> find(String key) {
        return raw(key).map(SecretResolver::resolve);
    }

    public String get(String key) {
        return find(key).orElseThrow(() ->
                new IllegalStateException("Required config key '" + key + "' is not set for env '"
                        + environment.id() + "'"));
    }

    public String get(String key, String defaultValue) {
        return find(key).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return find(key).map(Integer::parseInt).orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return find(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    /** True when the configured value is a secret reference (never print resolved value). */
    public boolean isSecretReference(String key) {
        return raw(key).map(SecretResolver::isReference).orElse(false);
    }
}
