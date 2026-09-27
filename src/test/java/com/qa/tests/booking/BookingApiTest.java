package com.qa.tests.booking;

import com.qa.framework.assertions.ResponseValidator;
import com.qa.framework.auth.BasicAuthStrategy;
import com.qa.framework.base.DestructiveTest;
import com.qa.framework.client.RequestSpecFactory;
import com.qa.framework.config.ConfigKeys;
import com.qa.framework.utils.JsonUtils;
import com.qa.tests.models.booking.Booking;
import com.qa.tests.models.booking.BookingId;
import com.qa.tests.models.booking.CreateBookingResponse;
import com.qa.tests.services.BookingService;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.qa.tests.data.BookingTestData.aBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/** Positive (happy-path) scenarios for the booking lifecycle. */
@Feature("Bookings")
@DisplayName("Bookings – positive scenarios")
class BookingApiTest extends BookingTestBase {

    private int sla() {
        return config().getInt("sla.response.ms", 5000);
    }

    // ============================================================ read-only (prod-safe)

    @Test
    @Tag("smoke")
    @Story("List bookings")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("GET /booking returns a list of booking ids")
    void listBookings() {
        List<BookingId> ids = List.of(ResponseValidator.of(bookings.getAllIds())
                .statusCode(200)
                .matchesSchema("schemas/booking-ids-schema.json")
                .respondedWithin(sla())
                .as(BookingId[].class));

        assertThat(ids).isNotEmpty().allSatisfy(id -> assertThat(id.bookingid()).isPositive());
    }

    @Test
    @Tag("smoke")
    @Story("Get booking")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("GET /booking/{id} for an existing booking matches the schema")
    void getExistingBooking() {
        int anyId = bookings.getAllIds().then().statusCode(200).extract().path("[0].bookingid");

        ResponseValidator.of(bookings.getById(anyId))
                .statusCode(200)
                .matchesSchema("schemas/booking-schema.json")
                .respondedWithin(sla());
    }

    // ============================================================ create

    @Test
    @Tag("smoke")
    @Tag("regression")
    @DestructiveTest
    @Story("Create booking")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("POST /booking creates a booking and echoes it back")
    void createBooking() {
        Booking request = aBooking().build();

        CreateBookingResponse created = ResponseValidator.of(bookings.create(request))
                .statusCode(200)
                .matchesSchema("schemas/create-booking-response-schema.json")
                .respondedWithin(sla())
                .as(CreateBookingResponse.class);
        registerForCleanup(created.bookingid());

        assertThat(created.bookingid()).isPositive();
        assertThat(created.booking()).as("echoed booking").isEqualTo(request);
    }

    @Test
    @Tag("regression")
    @DestructiveTest
    @Story("Create booking")
    @DisplayName("POST /booking with payload from a JSON test-data file")
    void createBookingFromJsonFile() {
        Booking request = JsonUtils.readResource("testdata/booking.json", Booking.class);

        CreateBookingResponse created = ResponseValidator.of(bookings.create(request))
                .statusCode(200)
                .as(CreateBookingResponse.class);
        registerForCleanup(created.bookingid());

        assertThat(created.booking()).isEqualTo(request);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @Tag("regression")
    @DestructiveTest
    @Story("Create booking")
    @Description("Boundary and variation data a real customer could send – all must be accepted and stored as sent.")
    @MethodSource("validBookingVariations")
    void createBookingVariations(String scenario, Booking request) {
        int id = givenBooking(request);

        Booking stored = ResponseValidator.of(bookings.getById(id))
                .statusCode(200)
                .as(Booking.class);

        assertThat(stored).as("stored booking for: " + scenario).isEqualTo(request);
    }

    static Stream<Arguments> validBookingVariations() {
        return Stream.of(
                Arguments.of("deposit not paid", aBooking().withDepositPaid(false).build()),
                Arguments.of("no additional needs (optional field omitted)", aBooking().withoutAdditionalNeeds().build()),
                Arguments.of("single-night stay", aBooking().withStay(3, 1).build()),
                Arguments.of("long stay – 30 nights", aBooking().withStay(10, 30).build()),
                Arguments.of("large total price", aBooking().withTotalPrice(999_999).build()),
                Arguments.of("hyphen and apostrophe in names",
                        aBooking().withFirstname("Mary-Jane").withLastname("O'Brien-" + System.nanoTime()).build()),
                Arguments.of("non-ASCII characters in names", aBooking().withFirstname("José").build()),
                Arguments.of("long additional needs text", aBooking().withAdditionalNeeds("Late check-in, ".repeat(20)).build())
        );
    }

    // ============================================================ read after write

    @Test
    @Tag("regression")
    @DestructiveTest
    @Story("Get booking")
    @DisplayName("Created booking can be read back unchanged")
    void createdBookingIsPersisted() {
        Booking request = aBooking().build();
        int id = givenBooking(request);

        Booking stored = ResponseValidator.of(bookings.getById(id))
                .statusCode(200)
                .matchesSchema("schemas/booking-schema.json")
                .as(Booking.class);

        assertThat(stored).isEqualTo(request);
    }

    @Test
    @Tag("regression")
    @DestructiveTest
    @Story("Search bookings")
    @DisplayName("Search by first + last name finds the new booking")
    void searchByNameFindsBooking() {
        Booking request = aBooking().build();       // last name is unique per test run
        int id = givenBooking(request);

        ResponseValidator.of(bookings.searchByName(request.firstname(), request.lastname()))
                .statusCode(200)
                .body("bookingid", hasItem(id));
    }

    @Test
    @Tag("regression")
    @Story("Search bookings")
    @DisplayName("Search by a name that doesn't exist returns an empty list")
    void searchByUnknownNameReturnsEmptyList() {
        ResponseValidator.of(bookings.searchByName("Nobody", "DoesNotExist-" + System.nanoTime()))
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    // ============================================================ update

    @Test
    @Tag("regression")
    @DestructiveTest
    @Story("Update booking")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PUT /booking/{id} with token replaces the booking")
    void fullUpdateWithToken() {
        Booking original = aBooking().build();
        int id = givenBooking(original);
        Booking changed = aBooking()
                .withLastname(original.lastname())
                .withTotalPrice(750)
                .withStay(7, 5)
                .withAdditionalNeeds("Lunch")
                .build();

        Booking updated = ResponseValidator.of(bookings.update(id, changed))
                .statusCode(200)
                .as(Booking.class);
        assertThat(updated).isEqualTo(changed);

        Booking stored = bookings.getById(id).as(Booking.class);
        assertThat(stored).as("state after update").isEqualTo(changed);
    }

    @Test
    @Tag("regression")
    @DestructiveTest
    @Story("Update booking")
    @DisplayName("PATCH /booking/{id} changes only the given fields")
    void partialUpdateChangesOnlyGivenFields() {
        Booking original = aBooking().build();
        int id = givenBooking(original);

        Booking patched = ResponseValidator.of(bookings.partialUpdate(id,
                        Map.of("totalprice", 999, "additionalneeds", "Dinner")))
                .statusCode(200)
                .as(Booking.class);

        assertThat(patched.totalprice()).isEqualTo(999);
        assertThat(patched.additionalneeds()).isEqualTo("Dinner");
        assertThat(patched)
                .as("untouched fields keep their values")
                .usingRecursiveComparison()
                .ignoringFields("totalprice", "additionalneeds")
                .isEqualTo(original);
    }

    @Test
    @Tag("regression")
    @DestructiveTest
    @Story("Update booking")
    @DisplayName("PUT /booking/{id} also accepts Basic auth instead of a token")
    void fullUpdateWithBasicAuth() {
        int id = givenBooking(aBooking().build());
        BookingService withBasicAuth = new BookingService(RequestSpecFactory.build(new BasicAuthStrategy(
                config().get(ConfigKeys.AUTH_USERNAME), () -> config().get(ConfigKeys.AUTH_PASSWORD))));
        Booking changed = aBooking().withTotalPrice(1200).build();

        ResponseValidator.of(withBasicAuth.update(id, changed))
                .statusCode(200)
                .body("totalprice", equalTo(1200));
    }

    // ============================================================ delete

    @Test
    @Tag("regression")
    @DestructiveTest
    @Story("Delete booking")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("DELETE /booking/{id} removes the booking (returns 201 – API quirk)")
    @Description("Restful-Booker answers a successful DELETE with 201 Created instead of 200/204. "
            + "After deletion the booking must no longer be retrievable.")
    void deleteBooking() {
        int id = givenBooking(aBooking().build());

        ResponseValidator.of(bookings.deleteById(id)).statusCode(201);
        alreadyDeleted(id);

        ResponseValidator.of(bookings.getById(id)).statusCode(404);
    }

    // ============================================================ end-to-end

    @Test
    @Tag("regression")
    @DestructiveTest
    @Story("End-to-end")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Customer journey: book → view → change dates → add request → cancel")
    void fullBookingLifecycle() {
        // 1. Customer books a stay
        Booking booking = aBooking().withStay(14, 3).build();
        int id = givenBooking(booking);

        // 2. Views it
        assertThat(bookings.getById(id).as(Booking.class)).isEqualTo(booking);

        // 3. Extends the stay by two nights (price goes up)
        Booking extended = aBooking().withLastname(booking.lastname()).withStay(14, 5).withTotalPrice(800).build();
        ResponseValidator.of(bookings.update(id, extended)).statusCode(200);

        // 4. Adds a special request
        ResponseValidator.of(bookings.partialUpdate(id, Map.of("additionalneeds", "Airport pickup")))
                .statusCode(200)
                .body("additionalneeds", equalTo("Airport pickup"))
                .body("totalprice", equalTo(800));

        // 5. Cancels
        ResponseValidator.of(bookings.deleteById(id)).statusCode(201);
        alreadyDeleted(id);
        ResponseValidator.of(bookings.getById(id)).statusCode(404);
    }
}
