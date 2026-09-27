package com.qa.tests.booking;

import com.qa.framework.assertions.ResponseValidator;
import com.qa.framework.base.BaseApiTest;
import com.qa.framework.config.ConfigKeys;
import com.qa.tests.models.booking.AuthResponse;
import com.qa.tests.services.AuthService;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@Epic("Restful-Booker")
@Feature("Authentication")
@DisplayName("Auth – POST /auth")
class AuthApiTest extends BaseApiTest {

    private final AuthService auth = new AuthService();

    // ---------------------------------------------------------------- positive

    @Test
    @Tag("smoke")
    @Story("Valid login")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Valid credentials return a token")
    void validCredentialsReturnToken() {
        // The request body (with the password) is attached to the Allure report – never do that with prod secrets.
        assumeFalse(env().isProd(), "Skipped on prod so the prod password never lands in the report");

        AuthResponse body = ResponseValidator.of(auth.createToken(
                        config().get(ConfigKeys.AUTH_USERNAME), config().get(ConfigKeys.AUTH_PASSWORD)))
                .statusCode(200)
                .contentType("application/json")
                .as(AuthResponse.class);

        assertThat(body.token()).as("token").isNotBlank().hasSizeGreaterThanOrEqualTo(10);
        assertThat(body.reason()).as("error reason").isNull();
    }

    // ---------------------------------------------------------------- negative

    @ParameterizedTest(name = "[{index}] {0}")
    @Tag("regression")
    @Tag("negative")
    @Story("Invalid login")
    @Description("Restful-Booker returns HTTP 200 (not 401) for bad credentials, with {\"reason\":\"Bad credentials\"}. "
            + "The test pins that behaviour and, more importantly, checks that NO token is issued.")
    @CsvSource(delimiter = '|', quoteCharacter = '"', textBlock = """
            wrong password               | admin              | wrongPassword
            wrong username               | notadmin           | password123
            username is case-sensitive   | ADMIN              | password123
            password is case-sensitive   | admin              | PASSWORD123
            empty username and password  | ""                 | ""
            password with trailing space | admin              | "password123 "
            SQL injection attempt        | admin' OR '1'='1   | anything
            """)
    void invalidCredentialsAreRejected(String scenario, String username, String password) {
        AuthResponse body = ResponseValidator.of(auth.createToken(username, password))
                .statusCode(200)
                .as(AuthResponse.class);

        assertThat(body.token()).as("token must NOT be issued").isNull();
        assertThat(body.reason()).isEqualTo("Bad credentials");
    }

    @Test
    @Tag("regression")
    @Tag("negative")
    @Story("Invalid login")
    @DisplayName("Missing username/password fields – no token issued")
    void missingFieldsAreRejected() {
        AuthResponse body = ResponseValidator.of(auth.createTokenRaw(Map.of()))
                .statusCode(200)
                .as(AuthResponse.class);

        assertThat(body.token()).isNull();
        assertThat(body.reason()).isEqualTo("Bad credentials");
    }
}
