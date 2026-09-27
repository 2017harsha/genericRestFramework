package com.qa.framework.secrets;

/** A backend that can resolve a secret reference such as {@code ssm:/app/prod/password}. */
public interface SecretProvider {

    /** Prefix handled by this provider, including the colon, e.g. {@code "ssm:"}. */
    String prefix();

    /** Resolve the reference (with the prefix already stripped). */
    String resolve(String reference);
}
