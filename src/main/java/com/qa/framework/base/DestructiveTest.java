package com.qa.framework.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks tests that create/modify/delete data. They are <b>skipped on prod</b> unless
 * {@code tests.allow.destructive=true} is set explicitly. Also tagged "destructive"
 * so they can be excluded with {@code -DexcludedGroups=destructive}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("destructive")
@ExtendWith(DestructiveTestCondition.class)
public @interface DestructiveTest {
}
