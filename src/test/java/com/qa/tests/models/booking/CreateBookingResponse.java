package com.qa.tests.models.booking;

/** Response of POST /booking: {"bookingid": 123, "booking": {...}}. */
public record CreateBookingResponse(Integer bookingid, Booking booking) {
}
