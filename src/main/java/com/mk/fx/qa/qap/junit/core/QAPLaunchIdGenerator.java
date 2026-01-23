package com.mk.fx.qa.qap.junit.core;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Generates and manages unique launch identifiers for test runs.
 * Launch IDs are stored as system properties and follow the format: "base-uuid"
 * where base is a configurable prefix and uuid is a 12-character random string.
 *
 * Thread-safety: generateIfAbsent() is synchronized to prevent races in parallel test execution.
 */
public class QAPLaunchIdGenerator {

  private static final String SYSTEM_PROPERTY_LAUNCH_ID = SystemProperties.LAUNCH_ID;
  private static final int MAX_LAUNCH_ID_LENGTH = 50;
  private static final int UUID_LENGTH = 12;
  
  /**
   * Pre-compiled regex pattern for launch ID validation.
   * Matches format: "anystring-12+alphanumericcharacters"
   * Compiled once for performance in high-concurrency scenarios.
   */
  private static final Pattern LAUNCH_ID_PATTERN = 
      Pattern.compile(".+[-a-zA-Z0-9]{" + UUID_LENGTH + ",}");

  public void generateLaunchId() {
    String current = System.getProperty(SYSTEM_PROPERTY_LAUNCH_ID);

    if (isFullLaunchId(current)) {
      return;
    }

    String base = extractBase(current);
    String launchId = base + "-" + generateShortUUID();
    System.setProperty(SYSTEM_PROPERTY_LAUNCH_ID, truncate(launchId));
  }

  public String getLaunchId() {
    return System.getProperty(SYSTEM_PROPERTY_LAUNCH_ID);
  }

  /**
   * Generates a launch id only if not already present, with a simple synchronization to avoid
   * check-then-act races across threads within the same JVM.
   */
  public synchronized void generateIfAbsent() {
    String current = getLaunchId();
    if (!isFullLaunchId(current)) {
      generateLaunchId();
    }
  }

  /**
   * Checks if the given value is a complete launch ID (contains base + UUID suffix).
   * Uses pre-compiled regex pattern for performance.
   *
   * @param value the value to check
   * @return true if the value is a valid, complete launch ID
   */
  private boolean isFullLaunchId(String value) {
    return value != null && LAUNCH_ID_PATTERN.matcher(value).matches();
  }

  /** Extracts the base from the existing property or uses a default if empty. */
  private String extractBase(String current) {
    if (current == null || current.isBlank()) {
      return "TestLaunch";
    }
    return current.split("-")[0];
  }

  /** Generates a short random UUID string with no dashes. */
  private String generateShortUUID() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, UUID_LENGTH);
  }

  /** Truncates the launch ID to the max length, removing trailing dashes if needed. */
  private String truncate(String launchId) {
    if (launchId.length() > MAX_LAUNCH_ID_LENGTH) {
      return launchId.substring(0, MAX_LAUNCH_ID_LENGTH).replaceAll("-+$", "");
    }
    return launchId;
  }
}
