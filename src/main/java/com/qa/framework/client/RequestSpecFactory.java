package com.qa.framework.client;

import com.qa.framework.auth.AuthFactory;
import com.qa.framework.auth.AuthStrategy;
import com.qa.framework.config.ConfigManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.specification.RequestSpecification;
import com.qa.framework.utils.JsonUtils;

import java.util.Locale;
import java.util.Set;

import static com.qa.framework.config.ConfigKeys.*;

/**
 * Builds the shared {@link RequestSpecification}: base URI/path, JSON content type, timeouts,
 * Allure request/response attachments, console logging (with secrets masked) and auth.
 */
public final class RequestSpecFactory {

    /** Headers never printed to the console log. */
    public static final Set<String> SENSITIVE_HEADERS =
            Set.of("Authorization", "Cookie", "Set-Cookie", "x-api-key", "Proxy-Authorization");

    private static volatile RequestSpecification defaultSpec;

    private RequestSpecFactory() {
    }

    /** The spec for the configured environment (built once, reused by all tests). */
    public static RequestSpecification defaultSpec() {
        if (defaultSpec == null) {
            synchronized (RequestSpecFactory.class) {
                if (defaultSpec == null) {
                    defaultSpec = build(AuthFactory.fromConfig());
                }
            }
        }
        return defaultSpec;
    }

    /** Spec for the main API with NO authentication – for negative "401/403 without token" tests. */
    public static RequestSpecification unauthenticated() {
        return build(AuthStrategy.NONE);
    }

    /**
     * Spec for a secondary service configured as {@code services.<name>.base.url}
     * (e.g. a second microservice or a third-party API). No authentication is applied.
     */
    public static RequestSpecification forService(String name) {
        return build(ConfigManager.get().get("services." + name + ".base.url"), AuthStrategy.NONE);
    }

    /** Build a spec with a different auth strategy, e.g. to test a second user or unauthenticated access. */
    public static RequestSpecification build(AuthStrategy auth) {
        return build(ConfigManager.get().get(BASE_URL), auth);
    }

    /** Build a spec for any base URL with the given auth strategy. */
    public static RequestSpecification build(String baseUrl, AuthStrategy auth) {
        ConfigManager cfg = ConfigManager.get();

        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setBasePath(cfg.get(BASE_PATH, ""))
                .setContentType(ContentType.JSON)
                // Exact media type: ContentType.JSON would send "application/json, application/javascript,
                // text/javascript, text/json" and strict APIs (e.g. Restful-Booker → 418) reject that.
                .setAccept("application/json")
                .setConfig(restAssuredConfig(cfg))
                .addFilter(new AllureRestAssured());

        if (cfg.getBoolean(RELAXED_HTTPS, false)) {
            builder.setRelaxedHTTPSValidation();
        }

        switch (cfg.get(LOG_MODE, "failures").toLowerCase(Locale.ROOT)) {
            case "all" -> builder
                    .addFilter(new RequestLoggingFilter(LogDetail.ALL))
                    .addFilter(new ResponseLoggingFilter(LogDetail.ALL));
            case "none" -> { /* quiet */ }
            default -> RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
        }

        return auth.apply(builder.build());
    }

    private static RestAssuredConfig restAssuredConfig(ConfigManager cfg) {
        int connectTimeout = cfg.getInt(CONNECT_TIMEOUT_MS, 15_000);
        int readTimeout = cfg.getInt(READ_TIMEOUT_MS, 30_000);
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", connectTimeout)
                        .setParam("http.socket.timeout", readTimeout))
                .logConfig(LogConfig.logConfig()
                        .blacklistHeaders(SENSITIVE_HEADERS)
                        .enablePrettyPrinting(true))
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> JsonUtils.mapper()));
    }
}
