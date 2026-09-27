package com.qa.tests.booking;

import com.qa.framework.assertions.ResponseValidator;
import com.qa.framework.auth.HeaderTokenAuthStrategy;
import com.qa.framework.base.DestructiveTest;
import com.qa.framework.client.RequestSpecFactory;
import com.qa.framework.utils.JsonUtils;
import com.qa.tests.models.booking.Booking;
import com.qa.tests.services.BookingService;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.qa.tests.data.BookingTestData.aBooking;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Negative scenarios: invalid input, missing/invalid auth, unknown resources and the API's
 * deliberate quirks. Where Restful-Booker's behaviour differs from good REST practice the test
 * pins the ACTUAL behaviour and says so; real defects are kept as @Disabled tests with an issue link.
 */
@Feature("Bookings")
@Tag("negative")
@DisplayName("Bookings – negative scenarios")
class BookingNegativeApiTest extends BookingTestBase {

    private final BookingService noAuth = new BookingService(RequestSpecFactory.unauthenticated());
    private final BookingService invalidToken = new BookingService(RequestSpecFactory.build(
            new HeaderTokenAuthStrategy("Cookie", "token=", () -> "invalid-or-expired-token")));

    private static final int NON_EXISTENT_ID = 999_999_999;

    // ================================================================ not found / bad ids

    @Nested
    @DisplayName("Unknown or malformed booking ids")
    class UnknownIds {

        @Test
        @Tag("regression")
        @Story("Get booking")
        @DisplayName("GET a non-existent booking → 404")
        void getNonExistentBooking() {
            ResponseValidator.of(bookings.getById(NON_EXISTENT_ID))
                    .statusCode(404)
                    .bodyContains("Not Found");
        }

        @ParameterizedTest(name = "GET /booking/{0} → 404")
        @Tag("regression")
        @Story("Get booking")
        @ValueSource(strings = {"abc", "-1", "0", "%20"})
        void getWithMalformedIdReturns404(String badId) {
            ResponseValidator.of(bookings.getById(badId)).statusCode(404);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Get booking")
        @DisplayName("GET /booking/{id}.5 – decimal id is truncated to the whole number (API quirk, 404 expected)")
        @Description("Restful-Booker parses only the leading integer of the id, so /booking/7.5 returns booking 7 "
                + "with 200 instead of rejecting the malformed id. The test pins that behaviour.")
        void decimalIdIsTruncatedToWholeNumber() {
            Booking booking = aBooking().build();
            int id = givenBooking(booking);

            Booking returned = ResponseValidator.of(bookings.getById(id + ".5"))
                    .statusCode(200)
                    .as(Booking.class);

            assertThat(returned).as("booking returned for id '" + id + ".5'").isEqualTo(booking);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Update booking")
        @DisplayName("PUT a non-existent booking (with token) → 405 (API quirk, 404 expected)")
        void updateNonExistentBooking() {
            ResponseValidator.of(bookings.update(NON_EXISTENT_ID, aBooking().build())).statusCode(405);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Delete booking")
        @DisplayName("DELETE a non-existent booking → 405 (API quirk, 404 expected)")
        void deleteNonExistentBooking() {
            ResponseValidator.of(bookings.deleteById(NON_EXISTENT_ID)).statusCode(405);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Delete booking")
        @DisplayName("DELETE the same booking twice → second call 405")
        void deleteTwice() {
            int id = givenBooking(aBooking().build());
            ResponseValidator.of(bookings.deleteById(id)).statusCode(201);
            alreadyDeleted(id);

            ResponseValidator.of(bookings.deleteById(id)).statusCode(405);
        }
    }

    // ================================================================ authorisation

    @Nested
    @Tag("security")
    @DisplayName("Authorisation on write operations")
    class Authorisation {

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Security")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("PUT without token → 403 and booking is unchanged")
        void updateWithoutTokenIsForbidden() {
            Booking original = aBooking().build();
            int id = givenBooking(original);

            ResponseValidator.of(noAuth.update(id, aBooking().withTotalPrice(1).build()))
                    .statusCode(403)
                    .bodyContains("Forbidden");

            assertThat(bookings.getById(id).as(Booking.class)).as("booking after rejected update").isEqualTo(original);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Security")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("PUT with an invalid token → 403")
        void updateWithInvalidTokenIsForbidden() {
            int id = givenBooking(aBooking().build());

            ResponseValidator.of(invalidToken.update(id, aBooking().build())).statusCode(403);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Security")
        @DisplayName("PATCH without token → 403 and price is unchanged")
        void partialUpdateWithoutTokenIsForbidden() {
            Booking original = aBooking().withTotalPrice(500).build();
            int id = givenBooking(original);

            ResponseValidator.of(noAuth.partialUpdate(id, Map.of("totalprice", 1))).statusCode(403);

            assertThat(bookings.getById(id).as(Booking.class).totalprice()).isEqualTo(500);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Security")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("DELETE without token → 403 and booking still exists")
        void deleteWithoutTokenIsForbidden() {
            int id = givenBooking(aBooking().build());

            ResponseValidator.of(noAuth.deleteById(id)).statusCode(403);

            ResponseValidator.of(bookings.getById(id)).statusCode(200);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Security")
        @DisplayName("DELETE with an invalid token → 403 and booking still exists")
        void deleteWithInvalidTokenIsForbidden() {
            int id = givenBooking(aBooking().build());

            ResponseValidator.of(invalidToken.deleteById(id)).statusCode(403);

            ResponseValidator.of(bookings.getById(id)).statusCode(200);
        }

        @Test
        @Tag("regression")
        @Story("Security")
        @DisplayName("Reading bookings needs no token (public endpoint)")
        void readIsPublic() {
            ResponseValidator.of(noAuth.getAllIds()).statusCode(200);
        }
    }

    // ================================================================ invalid payloads

    @Nested
    @DisplayName("Invalid create payloads")
    class InvalidPayloads {

        @ParameterizedTest(name = "POST /booking without \"{0}\" → rejected (500, API quirk – 400 expected)")
        @Tag("regression")
        @DestructiveTest
        @Story("Create booking – validation")
        @Description("Every mandatory field is removed in turn. Restful-Booker answers 500 Internal Server Error "
                + "instead of 400 Bad Request – the important part is that no booking is created.")
        @ValueSource(strings = {"firstname", "lastname", "totalprice", "depositpaid", "bookingdates"})
        void missingMandatoryFieldIsRejected(String field) {
            Map<String, Object> body = asMap(aBooking().build());
            body.remove(field);

            Response response = bookings.create(body);
            if (response.statusCode() == 200) {                       // don't leave junk if the API accepts it
                registerForCleanup(response.path("bookingid"));
            }
            ResponseValidator.of(response).statusCode(500);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Create booking – validation")
        @DisplayName("POST /booking with an empty JSON object → 500 (API quirk)")
        void emptyObjectIsRejected() {
            ResponseValidator.of(bookings.create(Map.of())).statusCode(500);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Create booking – validation")
        @DisplayName("POST /booking with malformed JSON → 400")
        void malformedJsonIsRejected() {
            ResponseValidator.of(bookings.createRaw("{\"firstname\": \"Harsha\", \"lastname\": "))
                    .statusCode(400);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Create booking – validation")
        @Issue("RB-101")
        @Disabled("Known defect RB-101: API accepts check-out before check-in (returns 200). Enable when fixed.")
        @DisplayName("POST /booking with check-out before check-in → 400")
        void checkoutBeforeCheckinIsRejected() {
            Booking invalid = aBooking().withDates("2026-12-10", "2026-12-01").build();

            Response response = bookings.create(invalid);
            if (response.statusCode() == 200) {
                registerForCleanup(response.path("bookingid"));
            }
            ResponseValidator.of(response).statusCode(400);
        }

        @Test
        @Tag("regression")
        @DestructiveTest
        @Story("Create booking – validation")
        @Issue("RB-102")
        @Disabled("Known defect RB-102: API accepts a negative total price. Enable when fixed.")
        @DisplayName("POST /booking with a negative total price → 400")
        void negativePriceIsRejected() {
            Response response = bookings.create(aBooking().withTotalPrice(-100).build());
            if (response.statusCode() == 200) {
                registerForCleanup(response.path("bookingid"));
            }
            ResponseValidator.of(response).statusCode(400);
        }
    }

    // ================================================================ content negotiation

    @Test
    @Tag("regression")
    @Story("Content negotiation")
    @DisplayName("GET /booking/{id} with an unsupported Accept header → 418 I'm a teapot (API quirk)")
    @Description("Restful-Booker only serves application/json and application/xml; anything else gets 418.")
    void unsupportedAcceptHeader() {
        int anyId = bookings.getAllIds().then().statusCode(200).extract().path("[0].bookingid");

        ResponseValidator.of(bookings.getByIdAccepting(anyId, "text/plain")).statusCode(418);
    }

    // ---------------------------------------------------------------- helpers

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Booking booking) {
        return new LinkedHashMap<>(JsonUtils.mapper().convertValue(booking, Map.class));
    }
}
