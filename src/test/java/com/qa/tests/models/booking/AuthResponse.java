package com.qa.tests.models.booking;

/** POST /auth returns either {"token": "..."} or {"reason": "Bad credentials"} (always HTTP 200). */
public record AuthResponse(String token, String reason) {
}
