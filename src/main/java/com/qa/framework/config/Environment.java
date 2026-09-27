package com.qa.framework.config;

import java.util.Arrays;
import java.util.Locale;

/**
 * Supported target environments. Selected with {@code -Denv=qa|dev|prod}
 * (or the {@code ENV} environment variable). Defaults to {@link #QA}.
 */
public enum Environment {
    QA, DEV, PROD;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isProd() {
        return this == PROD;
    }

    public static Environment from(String value) {
        if (value == null || value.isBlank()) {
            return QA;
        }
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown environment '" + value + "'. Allowed: " + Arrays.toString(values())));
    }
}
