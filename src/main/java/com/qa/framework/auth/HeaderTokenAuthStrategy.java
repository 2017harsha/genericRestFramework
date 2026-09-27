package com.qa.framework.auth;

import io.restassured.specification.RequestSpecification;

import java.util.function.Supplier;

/** Sends a token in a header, e.g. {@code Authorization: Bearer <token>} or {@code x-api-key: <key>}. */
public class HeaderTokenAuthStrategy implements AuthStrategy {

    private final String headerName;
    private final String prefix;
    private final Supplier<String> token;

    public HeaderTokenAuthStrategy(String headerName, String prefix, Supplier<String> token) {
        this.headerName = headerName;
        this.prefix = prefix == null ? "" : prefix;
        this.token = token;
    }

    @Override
    public RequestSpecification apply(RequestSpecification request) {
        return request.header(headerName, prefix + token.get());
    }
}
