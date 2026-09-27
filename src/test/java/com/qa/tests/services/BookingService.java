package com.qa.tests.services;

import com.qa.framework.client.BaseService;
import com.qa.tests.models.booking.CreateBookingResponse;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service object for Restful-Booker /booking.
 * <ul>
 *   <li>{@code new BookingService()} – authenticated with the token from config (Cookie: token=...)</li>
 *   <li>{@code new BookingService(RequestSpecFactory.unauthenticated())} – no token, for negative tests</li>
 * </ul>
 * Methods accept {@code Object} bodies so tests can also send invalid payloads (maps, raw strings).
 */
public class BookingService extends BaseService {

    public BookingService() {
        super("/booking");
    }

    public BookingService(RequestSpecification spec) {
        super("/booking", spec);
    }

    @Step("List all booking ids")
    public Response getAllIds() {
        return get("");
    }

    @Step("Search bookings by name {firstname} {lastname}")
    public Response searchByName(String firstname, String lastname) {
        return getWithQuery("", Map.of("firstname", firstname, "lastname", lastname));
    }

    @Step("Get booking {id}")
    public Response getById(Object id) {
        return get("/{id}", Map.of("id", id));
    }

    @Step("Get booking {id} with Accept: {accept}")
    public Response getByIdAccepting(Object id, String accept) {
        return request().accept(accept).pathParam("id", id).get("/booking/{id}");
    }

    @Step("Create booking")
    public Response create(Object booking) {
        return post("", booking);
    }

    @Step("Create booking with raw body")
    public Response createRaw(String rawBody) {
        return request().body(rawBody).post("/booking");
    }

    /** Creates a booking, asserts success and returns its id – for test set-up. */
    @Step("Given a booking exists")
    public int createAndGetId(Object booking) {
        Response response = create(booking);
        assertThat(response.statusCode()).as("create booking status").isEqualTo(200);
        return response.as(CreateBookingResponse.class).bookingid();
    }

    @Step("Update booking {id} (PUT)")
    public Response update(Object id, Object booking) {
        return put("/{id}", Map.of("id", id), booking);
    }

    @Step("Partially update booking {id} (PATCH)")
    public Response partialUpdate(Object id, Map<String, ?> fields) {
        return patch("/{id}", Map.of("id", id), fields);
    }

    @Step("Delete booking {id}")
    public Response deleteById(Object id) {
        return delete("/{id}", Map.of("id", id));
    }
}
