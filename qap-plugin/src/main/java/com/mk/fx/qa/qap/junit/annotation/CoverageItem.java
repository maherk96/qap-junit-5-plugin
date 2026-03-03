package com.mk.fx.qa.qap.junit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a coverage identifier for a test method.
 *
 * <p>Can be used multiple times on the same method to specify multiple coverage items:
 *
 * <pre>{@code
 * @Test
 * @CoverageItem("REQ-101")
 * @CoverageItem("STORY-456")
 * void testLogin() { }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CoverageItems.class)
public @interface CoverageItem {
  String value();
}
