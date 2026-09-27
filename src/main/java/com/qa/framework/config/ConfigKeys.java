package com.qa.framework.config;

/** All configuration keys understood by the framework core. Projects may add their own. */
public final class ConfigKeys {

    private ConfigKeys() {
    }

    // Target service
    public static final String BASE_URL = "base.url";
    public static final String BASE_PATH = "base.path";

    // Authentication: none | basic | bearer | token
    public static final String AUTH_TYPE = "auth.type";
    public static final String AUTH_USERNAME = "auth.username";
    public static final String AUTH_PASSWORD = "auth.password";
    public static final String AUTH_TOKEN = "auth.token";
    /** For auth.type=token: endpoint that exchanges username/password for a token. */
    public static final String AUTH_TOKEN_URL = "auth.token.url";
    /** For auth.type=token: JSON path of the token in the login response (default: token). */
    public static final String AUTH_TOKEN_JSON_PATH = "auth.token.json.path";
    /** Header used to send the token (default: Authorization with "Bearer " prefix). */
    public static final String AUTH_HEADER_NAME = "auth.header.name";
    public static final String AUTH_HEADER_PREFIX = "auth.header.prefix";

    // HTTP behaviour
    public static final String CONNECT_TIMEOUT_MS = "http.connect.timeout.ms";
    public static final String READ_TIMEOUT_MS = "http.read.timeout.ms";
    public static final String RELAXED_HTTPS = "http.relaxed.https";
    /** all | failures | none */
    public static final String LOG_MODE = "log.mode";

    // AWS
    public static final String AWS_REGION = "aws.region";
    public static final String AWS_PROFILE = "aws.profile";

    // Safety
    /** When false, tests annotated @DestructiveTest are skipped (default false on prod). */
    public static final String ALLOW_DESTRUCTIVE = "tests.allow.destructive";
}
