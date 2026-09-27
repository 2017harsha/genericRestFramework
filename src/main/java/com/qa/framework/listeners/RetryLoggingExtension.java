package com.qa.framework.listeners;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logs every attempt of a test when Surefire's automatic retry is on
 * ({@code rerunFailingTestsCount} = {@code retry.count}, default 2 → 1 run + 2 retries = 3 attempts).
 *
 * <pre>
 * WARN  RetryLog - [RETRY] BookingApiTest > createBooking() | attempt 1/3 FAILED | will retry (2 retries left) | cause: Expected status code &lt;200&gt; but was &lt;503&gt;
 * INFO  RetryLog - [RETRY] BookingApiTest > createBooking() | attempt 2/3 started (retry 1 of 2)
 * WARN  RetryLog - [RETRY] BookingApiTest > createBooking() | PASSED on attempt 2/3 after 1 failed attempt(s) -> FLAKY
 * ERROR RetryLog - [RETRY] BookingApiTest > createBooking() | FAILED on all 3 attempts (retried 2 times) -> marked FAILED | cause: ...
 * </pre>
 *
 * Surefire re-runs failed tests in the same JVM, so the attempt counter (keyed by the test's JUnit unique id)
 * survives between attempts. Each attempt also gets an "Attempt n of N" step in the Allure report.
 * Registered for every test through {@code @ExtendWith} on {@link com.qa.framework.base.BaseApiTest}.
 */
public class RetryLoggingExtension implements BeforeTestExecutionCallback, TestWatcher {

    private static final Logger LOG = LoggerFactory.getLogger("RetryLog");
    private static final Map<String, Integer> ATTEMPTS = new ConcurrentHashMap<>();

    /** Total attempts = first run + retries. Surefire passes retry.count as a system property. */
    private static int maxAttempts() {
        try {
            return Math.max(0, Integer.parseInt(System.getProperty("retry.count", "0").trim())) + 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        int attempt = ATTEMPTS.merge(context.getUniqueId(), 1, Integer::sum);
        int max = maxAttempts();
        if (max > 1) {
            Allure.step("Attempt " + attempt + " of " + max);
        }
        if (attempt > 1) {
            LOG.info("[RETRY] {} | attempt {}/{} started (retry {} of {})",
                    name(context), attempt, max, attempt - 1, max - 1);
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        int attempt = currentAttempt(context);
        if (attempt > 1) {
            LOG.warn("[RETRY] {} | PASSED on attempt {}/{} after {} failed attempt(s) -> FLAKY",
                    name(context), attempt, maxAttempts(), attempt - 1);
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        int attempt = currentAttempt(context);
        int max = maxAttempts();
        if (attempt < max) {
            LOG.warn("[RETRY] {} | attempt {}/{} FAILED | will retry ({} retr{} left) | cause: {}",
                    name(context), attempt, max, max - attempt, (max - attempt) == 1 ? "y" : "ies", shortCause(cause));
        } else if (max > 1) {
            LOG.error("[RETRY] {} | FAILED on all {} attempts (retried {} time{}) -> marked FAILED | cause: {}",
                    name(context), attempt, attempt - 1, (attempt - 1) == 1 ? "" : "s", shortCause(cause));
        } else {
            LOG.error("[RETRY] {} | FAILED (retries disabled, retry.count=0) | cause: {}",
                    name(context), shortCause(cause));
        }
    }

    private static int currentAttempt(ExtensionContext context) {
        return ATTEMPTS.getOrDefault(context.getUniqueId(), 1);
    }

    /** "BookingNegativeApiTest > UnknownIds > getWithMalformedIdReturns404(String) > GET /booking/abc -> 404" */
    private static String name(ExtensionContext context) {
        Deque<String> parts = new ArrayDeque<>();
        for (ExtensionContext c = context; c.getParent().isPresent(); c = c.getParent().get()) {   // stop at engine root
            boolean classNode = c.getTestMethod().isEmpty() && c.getTestClass().isPresent();
            parts.addFirst(classNode ? c.getRequiredTestClass().getSimpleName() : c.getDisplayName());
        }
        return String.join(" > ", parts);
    }

    private static String shortCause(Throwable cause) {
        if (cause == null) {
            return "-";
        }
        String msg = String.valueOf(cause.getMessage()).strip().replaceAll("\\s+", " ");
        String text = cause.getClass().getSimpleName() + ": " + msg;
        return text.length() > 250 ? text.substring(0, 250) + "..." : text;
    }
}
