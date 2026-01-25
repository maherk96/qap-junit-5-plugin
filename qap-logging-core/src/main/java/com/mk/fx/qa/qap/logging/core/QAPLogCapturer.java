package com.mk.fx.qa.qap.logging.core;

import java.util.List;

/**
 * Interface for capturing logs from different logging frameworks. Implementations are
 * framework-specific (Logback, Log4j2, etc.).
 *
 * <p>Implementations must be thread-safe as they may be used across multiple test threads in
 * parallel execution.
 */
public interface QAPLogCapturer {

  /**
   * Starts capturing logs for a specific test. Must be called before test execution begins.
   *
   * @param testId unique identifier for the test (typically the test case ID)
   * @param config configuration for log capture behavior
   */
  void startCapture(String testId, QAPLogCaptureConfig config);

  /**
   * Stops capturing logs for a specific test and returns the captured logs. Must be called after
   * test execution completes.
   *
   * @param testId unique identifier for the test
   * @return list of captured log entries, never null (may be empty)
   */
  List<QAPLogEntry> stopCapture(String testId);

  /**
   * Returns the name of the logging framework this capturer supports. Used for diagnostics and user
   * feedback.
   *
   * @return framework name (e.g., "Logback", "Log4j2", "JUL")
   */
  String getFrameworkName();

  /**
   * Checks if this capturer can be used in the current environment. Typically checks if the
   * required logging framework classes are available on the classpath.
   *
   * @return true if the logging framework is available and this capturer can be used
   */
  boolean isAvailable();

  /**
   * Returns the priority of this capturer when multiple capturers are available. Higher priority
   * capturers are preferred. Default priority is 0.
   *
   * @return priority value (higher = preferred)
   */
  default int getPriority() {
    return 0;
  }

  /**
   * Optional cleanup method called when the capturer is no longer needed. Implementations should
   * release resources, detach appenders, etc.
   */
  default void shutdown() {
    // Default: no-op
  }
}
