package com.qa.framework.auth;

import io.restassured.specification.RequestSpecification;

import java.util.function.Supplier;

/** Pre-emptive HTTP Basic auth. The password supplier is evaluated lazily (e.g. from AWS SSM). */
public class BasicAuthStrategy implements AuthStrategy {

    private final String username;
    private final Supplier<String> password;

    public BasicAuthStrategy(String username, Supplier<String> password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public RequestSpecification apply(RequestSpecification request) {
        return request.auth().preemptive().basic(username, password.get());
    }
}
