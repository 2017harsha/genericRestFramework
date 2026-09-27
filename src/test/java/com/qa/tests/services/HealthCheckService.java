package com.qa.tests.services;

import com.qa.framework.client.BaseService;
import com.qa.framework.client.RequestSpecFactory;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Restful-Booker GET /ping – health check, no auth needed. */
public class HealthCheckService extends BaseService {

    public HealthCheckService() {
        super("/ping", RequestSpecFactory.unauthenticated());
    }

    @Step("Ping the API")
    public Response ping() {
        return get("");
    }
}
