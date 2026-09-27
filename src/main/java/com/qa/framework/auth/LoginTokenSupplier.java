package com.qa.framework.auth;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Obtains a token once by POSTing {@code {"username": ..., "password": ...}} to a login endpoint
 * and caches it for the rest of the run. Override {@link #loginBody} for other payload shapes.
 */
public class LoginTokenSupplier implements Supplier<String> {

    private final String tokenUrl;
    private final String username;
    private final Supplier<String> password;
    private final String tokenJsonPath;
    private volatile String cachedToken;

    public LoginTokenSupplier(String tokenUrl, String username, Supplier<String> password, String tokenJsonPath) {
        this.tokenUrl = tokenUrl;
        this.username = username;
        this.password = password;
        this.tokenJsonPath = tokenJsonPath;
    }

    @Override
    public String get() {
        if (cachedToken == null) {
            synchronized (this) {
                if (cachedToken == null) {
                    cachedToken = fetchToken();
                }
            }
        }
        return cachedToken;
    }

    protected Object loginBody(String user, String pass) {
        return Map.of("username", user, "password", pass);
    }

    private String fetchToken() {
        // Deliberately NOT using the shared spec: the login call must not be logged with the password.
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(loginBody(username, password.get()))
                .post(tokenUrl);
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Login to " + tokenUrl + " failed with HTTP " + response.statusCode());
        }
        String token = response.jsonPath().getString(tokenJsonPath);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No token at JSON path '" + tokenJsonPath + "' in login response");
        }
        return token;
    }
}
