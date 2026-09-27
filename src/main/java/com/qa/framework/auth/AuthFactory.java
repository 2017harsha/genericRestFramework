package com.qa.framework.auth;

import com.qa.framework.config.ConfigManager;

import java.util.Locale;
import java.util.function.Supplier;

import static com.qa.framework.config.ConfigKeys.*;

/**
 * Builds the {@link AuthStrategy} described by config ({@code auth.type}):
 * <ul>
 *   <li>{@code none}   – no authentication (default)</li>
 *   <li>{@code basic}  – auth.username + auth.password</li>
 *   <li>{@code bearer} – static auth.token (e.g. an API key) sent in auth.header.name</li>
 *   <li>{@code token}  – log in at auth.token.url with username/password, reuse the returned token</li>
 * </ul>
 * Secrets are resolved lazily, so a qa run never touches AWS just because prod is configured with ssm:.
 */
public final class AuthFactory {

    private AuthFactory() {
    }

    public static AuthStrategy fromConfig() {
        ConfigManager cfg = ConfigManager.get();
        String type = cfg.get(AUTH_TYPE, "none").toLowerCase(Locale.ROOT);
        Supplier<String> password = () -> cfg.get(AUTH_PASSWORD);
        String header = cfg.get(AUTH_HEADER_NAME, "Authorization");
        String prefix = cfg.raw(AUTH_HEADER_PREFIX).orElse("Authorization".equalsIgnoreCase(header) ? "Bearer " : "");

        return switch (type) {
            case "none" -> AuthStrategy.NONE;
            case "basic" -> new BasicAuthStrategy(cfg.get(AUTH_USERNAME), password);
            case "bearer" -> new HeaderTokenAuthStrategy(header, prefix, () -> cfg.get(AUTH_TOKEN));
            case "token" -> new HeaderTokenAuthStrategy(header, prefix, new LoginTokenSupplier(
                    absolute(cfg, cfg.get(AUTH_TOKEN_URL)), cfg.get(AUTH_USERNAME), password,
                    cfg.get(AUTH_TOKEN_JSON_PATH, "token")));
            default -> throw new IllegalStateException("Unsupported auth.type '" + type
                    + "'. Use none | basic | bearer | token");
        };
    }

    /** auth.token.url may be relative ("/auth") – resolve it against base.url. */
    private static String absolute(ConfigManager cfg, String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = cfg.get(BASE_URL).replaceAll("/+$", "");
        return base + (url.startsWith("/") ? url : "/" + url);
    }
}
