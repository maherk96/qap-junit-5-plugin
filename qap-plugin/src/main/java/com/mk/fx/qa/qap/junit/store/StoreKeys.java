package com.mk.fx.qa.qap.junit.store;

/**
 * Centralized store keys for JUnit ExtensionContext stores.
 *
 * <p>Using this class instead of string literals provides:
 *
 * <ul>
 *   <li>Type safety and compile-time checking
 *   <li>IDE refactoring support
 *   <li>Clear documentation of all store keys in one place
 *   <li>Prevention of typos and key collisions
 * </ul>
 */
public final class StoreKeys {

  // ---- Class-level store keys ----

  /** Key for the main QAPJunitLaunch object (top-level only). */
  public static final String TEST_CLASS_DATA = "qap.testClassData";

  /** Key for the map of class nodes (QAPTestClass hierarchy). */
  public static final String CLASS_NODES = "qap.classNodes";

  /** Key for backward-compatible flat list of test descriptions. */
  public static final String METHOD_DESCRIPTIONS = "qap.methodDescriptions";

  // ---- Invocation-level store keys ----

  /** Key for the current test's QAPTest object (per invocation). */
  public static final String INVOCATION_TEST = "qap.invocation.test";

  /** Key for parameterized test index counter. */
  public static final String PARAM_INDEX = "qap.paramIndex";

  // ---- Method-level store keys ----

  /** Key for method-level fixture tracking. */
  public static final String METHOD_FIXTURES = "qap.method.fixtures";

  // ---- Fixture timing keys ----

  /** Key for fixture start time (epoch millis). */
  public static final String FIXTURE_START_TIME = "qap.fixture.startTime";

  /** Key for fixture start time (nanoTime for duration calculation). */
  public static final String FIXTURE_START_NANOS = "qap.fixture.startNanos";

  private StoreKeys() {
    throw new UnsupportedOperationException("Utility class - do not instantiate");
  }
}
