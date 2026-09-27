package com.qa.framework.auth;

import io.restassured.specification.RequestSpecification;

/** Applies authentication to an outgoing request. */
@FunctionalInterface
public interface AuthStrategy {

    RequestSpecification apply(RequestSpecification request);

    AuthStrategy NONE = request -> request;
}
