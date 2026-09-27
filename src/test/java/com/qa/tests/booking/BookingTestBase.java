package com.qa.tests.booking;

import com.qa.framework.base.BaseApiTest;
import com.qa.tests.models.booking.Booking;
import com.qa.tests.services.BookingService;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;

/**
 * Common set-up for Restful-Booker tests.
 * Every booking created through {@link #givenBooking(Booking)} is deleted after the test,
 * so runs don't leave junk data behind (important on shared QA environments).
 */
@Epic("Restful-Booker")
abstract class BookingTestBase extends BaseApiTest {

    protected final BookingService bookings = new BookingService();
    private final List<Integer> createdIds = new ArrayList<>();

    /** Creates a booking as test pre-condition and registers it for clean-up. */
    protected int givenBooking(Booking booking) {
        int id = bookings.createAndGetId(booking);
        createdIds.add(id);
        return id;
    }

    /** Register an id created inside the test itself (e.g. by the call under test). */
    protected void registerForCleanup(Integer id) {
        if (id != null) {
            createdIds.add(id);
        }
    }

    /** Test deleted the booking itself – nothing to clean up. */
    protected void alreadyDeleted(Integer id) {
        createdIds.remove(id);
    }

    @AfterEach
    @Step("Clean up test bookings")
    void deleteCreatedBookings() {
        for (Integer id : createdIds) {
            try {
                bookings.deleteById(id);
            } catch (RuntimeException e) {
                LOG.warn("Could not delete booking {}: {}", id, e.getMessage());
            }
        }
        createdIds.clear();
    }
}
