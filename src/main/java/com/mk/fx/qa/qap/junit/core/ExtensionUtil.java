package com.mk.fx.qa.qap.junit.core;

/** Utility class for handling extension-related operations. */
public class ExtensionUtil {

  private ExtensionUtil() {
    // Private constructor to prevent instantiation
  }

  /**
   * Retrieves the operating system version.
   *
   * @return the OS version as a string
   */
  public static String getOsVersion() {
    return System.getProperty("os.name") + " " + System.getProperty("os.version");
  }

  /**
   * Retrieves the JDK version.
   *
   * @return the JDK version as a string
   */
  public static String getJdkVersion() {
    return "JDK " + System.getProperty("java.version");
  }

  /**
   * Checks if the 'qap.regression' system property is set.
   *
   * @return true if the 'qap.regression' property is not null, false otherwise.
   */
  public static boolean isRegressionEnabled() {
    return System.getProperty("qap.regression") != null;
  }
}
