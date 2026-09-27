package com.qa.framework.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** Shared Jackson mapper plus helpers to read test data from {@code src/test/resources}. */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private JsonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialise " + value, e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot parse JSON into " + type.getSimpleName(), e);
        }
    }

    /** Load a classpath resource, e.g. {@code readResource("testdata/booking.json", Booking.class)}. */
    public static <T> T readResource(String resource, Class<T> type) {
        try (InputStream in = open(resource)) {
            return MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + resource, e);
        }
    }

    public static <T> T readResource(String resource, TypeReference<T> type) {
        try (InputStream in = open(resource)) {
            return MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + resource, e);
        }
    }

    private static InputStream open(String resource) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalArgumentException("Resource not found on classpath: " + resource);
        }
        return in;
    }
}
