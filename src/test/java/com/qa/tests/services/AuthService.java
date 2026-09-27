package com.qa.tests.services;

import com.qa.framework.client.BaseService;
import com.qa.framework.client.RequestSpecFactory;
import com.qa.tests.models.booking.AuthRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Restful-Booker POST /auth – called without an existing token. */
public class AuthService extends BaseService {

    public AuthService() {
        super("/auth", RequestSpecFactory.unauthenticated());
    }

    @Step("Request auth token for user {username}")
    public Response createToken(String username, String password) {
        return post("", new AuthRequest(username, password));
    }

    @Step("Request auth token with raw body")
    public Response createTokenRaw(Object body) {
        return post("", body);
    }
}
