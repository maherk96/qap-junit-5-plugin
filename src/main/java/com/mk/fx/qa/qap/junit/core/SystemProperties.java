package com.mk.fx.qa.qap.junit.core;

/**
 * Centralized constants for system property names used across the QAP JUnit extension.
 * Consolidates property names to avoid magic strings and enable easier refactoring.
 */
public final class SystemProperties {

  /** System property key for the test launch identifier. */
  public static final String LAUNCH_ID = "launchID";

  /** System property key to enable regression test mode. */
  public static final String QAP_REGRESSION = "qap.regression";

  /** System property key for Java version information. */
  public static final String JAVA_VERSION = "java.version";

  /** System property key for operating system name. */
  public static final String OS_NAME = "os.name";

  /** System property key for operating system version. */
  public static final String OS_VERSION = "os.version";

  /** System property key for system user name. */
  public static final String USER_NAME = "user.name";

  private SystemProperties() {
    // Prevent instantiation - constants class only
  }
}
