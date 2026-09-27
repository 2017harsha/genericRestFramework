package com.qa.framework.secrets;

/**
 * Resolves {@code env:VAR_NAME} from the OS environment. Typical use: a Jenkins
 * {@code withCredentials} block exports the variable for qa/dev runs.
 */
public class EnvVarSecretProvider implements SecretProvider {

    @Override
    public String prefix() {
        return "env:";
    }

    @Override
    public String resolve(String reference) {
        String value = System.getenv(reference);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(reference);
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Secret environment variable '" + reference + "' is not set");
        }
        return value;
    }
}
