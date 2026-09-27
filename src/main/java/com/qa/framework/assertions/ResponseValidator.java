package com.qa.framework.assertions;

import io.qameta.allure.Step;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.hamcrest.Matcher;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluent, Allure-reported checks on a response:
 * <pre>
 * ResponseValidator.of(response)
 *     .statusCode(200)
 *     .matchesSchema("schemas/post.json")
 *     .respondedWithin(2000)
 *     .body("title", equalTo("foo"));
 * </pre>
 */
public final class ResponseValidator {

    private final Response response;

    private ResponseValidator(Response response) {
        this.response = response;
    }

    public static ResponseValidator of(Response response) {
        return new ResponseValidator(response);
    }

    @Step("Status code is {expected}")
    public ResponseValidator statusCode(int expected) {
        response.then().statusCode(expected);
        return this;
    }

    @Step("Body matches JSON schema {schemaClasspath}")
    public ResponseValidator matchesSchema(String schemaClasspath) {
        response.then().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaClasspath));
        return this;
    }

    @Step("Response time below {maxMillis} ms")
    public ResponseValidator respondedWithin(long maxMillis) {
        assertThat(response.timeIn(TimeUnit.MILLISECONDS))
                .as("response time (ms)")
                .isLessThanOrEqualTo(maxMillis);
        return this;
    }

    @Step("Header {name} is present")
    public ResponseValidator hasHeader(String name) {
        assertThat(response.getHeader(name)).as("header " + name).isNotNull();
        return this;
    }

    @Step("Content type contains {expected}")
    public ResponseValidator contentType(String expected) {
        assertThat(response.getContentType()).contains(expected);
        return this;
    }

    @Step("Body field {path} matches")
    public ResponseValidator body(String path, Matcher<?> matcher) {
        response.then().body(path, matcher);
        return this;
    }

    @Step("Body contains text \"{expected}\"")
    public ResponseValidator bodyContains(String expected) {
        assertThat(response.asString()).as("response body").contains(expected);
        return this;
    }

    @Step("Body does not contain \"{unexpected}\"")
    public ResponseValidator bodyDoesNotContain(String unexpected) {
        assertThat(response.asString()).as("response body").doesNotContain(unexpected);
        return this;
    }

    /** Deserialize for further typed assertions. */
    public <T> T as(Class<T> type) {
        return response.as(type);
    }

    public Response response() {
        return response;
    }
}
