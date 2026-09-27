package com.qa.framework.client;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Base class for "service objects" – one class per API resource (e.g. UsersService, OrdersService).
 * Keeps HTTP plumbing out of the tests so tests read as business scenarios.
 *
 * <pre>
 * public class UsersService extends BaseService {
 *     public UsersService() { super("/users"); }
 *     public Response getUser(int id) { return get("/{id}", Map.of("id", id)); }
 * }
 * </pre>
 */
public abstract class BaseService {

    private final String resourcePath;
    private final RequestSpecification spec;

    protected BaseService(String resourcePath) {
        this(resourcePath, RequestSpecFactory.defaultSpec());
    }

    protected BaseService(String resourcePath, RequestSpecification spec) {
        this.resourcePath = resourcePath == null ? "" : resourcePath;
        this.spec = spec;
    }

    /** Fresh request pre-loaded with the shared spec. Use for anything the helpers below don't cover. */
    protected RequestSpecification request() {
        return given().spec(spec);
    }

    private String url(String path) {
        return resourcePath + (path == null ? "" : path);
    }

    @Step("GET {path}")
    protected Response get(String path) {
        return request().get(url(path));
    }

    @Step("GET {path} {pathParams}")
    protected Response get(String path, Map<String, ?> pathParams) {
        return request().pathParams(pathParams).get(url(path));
    }

    @Step("GET {path} with query {queryParams}")
    protected Response getWithQuery(String path, Map<String, ?> queryParams) {
        return request().queryParams(queryParams).get(url(path));
    }

    @Step("POST {path}")
    protected Response post(String path, Object body) {
        return request().body(body).post(url(path));
    }

    @Step("PUT {path} {pathParams}")
    protected Response put(String path, Map<String, ?> pathParams, Object body) {
        return request().pathParams(pathParams).body(body).put(url(path));
    }

    @Step("PATCH {path} {pathParams}")
    protected Response patch(String path, Map<String, ?> pathParams, Object body) {
        return request().pathParams(pathParams).body(body).patch(url(path));
    }

    @Step("DELETE {path} {pathParams}")
    protected Response delete(String path, Map<String, ?> pathParams) {
        return request().pathParams(pathParams).delete(url(path));
    }
}
