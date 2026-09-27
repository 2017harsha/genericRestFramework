package com.qa.tests.data;

import com.qa.framework.utils.RandomData;
import com.qa.tests.models.booking.Booking;
import com.qa.tests.models.booking.BookingDates;

import java.time.LocalDate;

/**
 * Test-data builder for bookings. Every booking gets a unique last name so tests can find
 * "their" booking on a shared public server and never collide with other runs.
 *
 * <pre>
 * Booking b = BookingTestData.aBooking().withTotalPrice(900).withoutAdditionalNeeds().build();
 * </pre>
 */
public final class BookingTestData {

    private String firstname = "Harsha";
    private String lastname = "V-" + RandomData.uniqueSuffix();
    private Integer totalprice = 500;
    private Boolean depositpaid = true;
    private String checkin = LocalDate.now().plusDays(7).toString();
    private String checkout = LocalDate.now().plusDays(11).toString();
    private String additionalneeds = "Breakfast";

    private BookingTestData() {
    }

    /** A valid booking with sensible defaults (check-in in 7 days, 4 nights, breakfast). */
    public static BookingTestData aBooking() {
        return new BookingTestData();
    }

    public BookingTestData withFirstname(String value) { this.firstname = value; return this; }
    public BookingTestData withLastname(String value) { this.lastname = value; return this; }
    public BookingTestData withTotalPrice(Integer value) { this.totalprice = value; return this; }
    public BookingTestData withDepositPaid(Boolean value) { this.depositpaid = value; return this; }
    public BookingTestData withAdditionalNeeds(String value) { this.additionalneeds = value; return this; }
    public BookingTestData withoutAdditionalNeeds() { this.additionalneeds = null; return this; }

    public BookingTestData withDates(String checkinDate, String checkoutDate) {
        this.checkin = checkinDate;
        this.checkout = checkoutDate;
        return this;
    }

    /** Stay of {@code nights} nights starting {@code daysFromToday} days from today. */
    public BookingTestData withStay(int daysFromToday, int nights) {
        LocalDate in = LocalDate.now().plusDays(daysFromToday);
        return withDates(in.toString(), in.plusDays(nights).toString());
    }

    public Booking build() {
        BookingDates dates = (checkin == null && checkout == null) ? null : new BookingDates(checkin, checkout);
        return new Booking(firstname, lastname, totalprice, depositpaid, dates, additionalneeds);
    }
}
