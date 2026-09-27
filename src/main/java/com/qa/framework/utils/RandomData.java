package com.qa.framework.utils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Small helpers for unique test data (keeps the framework free of heavy faker dependencies). */
public final class RandomData {

    private RandomData() {
    }

    public static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String email() {
        return "qa.auto+" + uniqueSuffix() + "@example.com";
    }

    public static String name(String prefix) {
        return prefix + "-" + uniqueSuffix();
    }

    public static int number(int minInclusive, int maxExclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxExclusive);
    }
}
