package com.qa.framework.secrets;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Turns secret references into real values and caches them for the JVM lifetime,
 * so each secret is fetched at most once per test run.
 *
 * <pre>
 *   auth.password=ssm:/my-app/prod/api-password   → AWS Parameter Store
 *   auth.password=env:QA_API_PASSWORD             → environment variable
 *   auth.password=plain-text                      → returned as is (avoid for real secrets)
 * </pre>
 *
 * Additional backends (Vault, Secrets Manager…) can be plugged in with {@link #register}.
 */
public final class SecretResolver {

    private static final List<SecretProvider> PROVIDERS = new CopyOnWriteArrayList<>(List.of(
            new AwsParameterStoreProvider(),
            new EnvVarSecretProvider()));
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private SecretResolver() {
    }

    public static void register(SecretProvider provider) {
        PROVIDERS.add(0, provider);
    }

    public static boolean isReference(String value) {
        return value != null && PROVIDERS.stream().anyMatch(p -> value.startsWith(p.prefix()));
    }

    public static String resolve(String value) {
        if (value == null) {
            return null;
        }
        for (SecretProvider provider : PROVIDERS) {
            if (value.startsWith(provider.prefix())) {
                return CACHE.computeIfAbsent(value,
                        v -> provider.resolve(v.substring(provider.prefix().length()).trim()));
            }
        }
        return value;
    }
}
