package com.qa.framework.base;

import com.qa.framework.config.ConfigManager;
import com.qa.framework.config.Environment;
import com.qa.framework.listeners.AllureEnvironmentWriter;
import com.qa.framework.listeners.RetryLoggingExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extend this in every test class. Gives access to config and the current environment
 * and makes sure the Allure "Environment" widget is populated once per run.
 * {@link RetryLoggingExtension} logs every attempt when a failing test is retried.
 */
@ExtendWith(RetryLoggingExtension.class)
public abstract class BaseApiTest {

    protected static final Logger LOG = LoggerFactory.getLogger(BaseApiTest.class);

    protected static ConfigManager config() {
        return ConfigManager.get();
    }

    protected static Environment env() {
        return ConfigManager.get().environment();
    }

    @BeforeAll
    static void frameworkSetUp() {
        AllureEnvironmentWriter.writeOnce();
    }
}
