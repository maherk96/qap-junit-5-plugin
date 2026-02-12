package com.mk.fx.qa.qap.junit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link CoverageItem} annotations.
 *
 * <p>This annotation is automatically applied when using {@code @CoverageItem} multiple times:
 *
 * <pre>{@code
 * @Test
 * @CoverageItem("REQ-100")
 * @CoverageItem("BUG-200")
 * void testMethod() { }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CoverageItems {
  CoverageItem[] value();
}
