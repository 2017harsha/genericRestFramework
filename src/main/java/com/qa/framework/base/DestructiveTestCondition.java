package com.qa.framework.base;

import com.qa.framework.config.ConfigKeys;
import com.qa.framework.config.ConfigManager;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Skips {@link DestructiveTest}s on prod unless explicitly allowed. */
public class DestructiveTestCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        ConfigManager cfg = ConfigManager.get();
        boolean allowed = cfg.getBoolean(ConfigKeys.ALLOW_DESTRUCTIVE, !cfg.environment().isProd());
        return allowed
                ? ConditionEvaluationResult.enabled("destructive tests allowed on " + cfg.environment().id())
                : ConditionEvaluationResult.disabled("destructive test skipped on " + cfg.environment().id()
                + " (set tests.allow.destructive=true to override)");
    }
}
