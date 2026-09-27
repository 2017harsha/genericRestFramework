package com.qa.tests.models.booking;

/**
 * Restful-Booker booking payload / response body.
 * A Java record: immutable, with equals/hashCode for easy "request == response" assertions.
 */
public record Booking(
        String firstname,
        String lastname,
        Integer totalprice,
        Boolean depositpaid,
        BookingDates bookingdates,
        String additionalneeds) {
}
