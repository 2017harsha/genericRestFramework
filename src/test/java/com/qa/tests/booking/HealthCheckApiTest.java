package com.qa.tests.booking;

import com.qa.framework.assertions.ResponseValidator;
import com.qa.framework.base.BaseApiTest;
import com.qa.tests.services.HealthCheckService;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Restful-Booker")
@Feature("Health check")
@DisplayName("Health check")
class HealthCheckApiTest extends BaseApiTest {

    private final HealthCheckService health = new HealthCheckService();

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("GET /ping – API is up")
    @Description("Restful-Booker answers the health check with 201 Created (not 200 – a documented quirk).")
    void apiIsUp() {
        ResponseValidator.of(health.ping())
                .statusCode(201)
                .bodyContains("Created")
                .respondedWithin(config().getInt("sla.response.ms", 5000));
    }
}
