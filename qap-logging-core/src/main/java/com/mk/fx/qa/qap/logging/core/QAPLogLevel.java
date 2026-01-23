package com.mk.fx.qa.qap.logging.core;

/**
 * Log levels for QAP log capture. Aligned with common logging frameworks (SLF4J, Log4j2, Logback).
 */
public enum QAPLogLevel {
  /** Trace level - most verbose */
  TRACE(0),

  /** Debug level - detailed information for debugging */
  DEBUG(1),

  /** Info level - informational messages */
  INFO(2),

  /** Warn level - warning messages */
  WARN(3),

  /** Error level - error messages */
  ERROR(4),

  /** Fatal level - critical errors (Log4j2 specific) */
  FATAL(5);

  private final int severity;

  QAPLogLevel(int severity) {
    this.severity = severity;
  }

  /**
   * Returns the numeric severity level (higher = more severe).
   *
   * @return severity value
   */
  public int getSeverity() {
    return severity;
  }

  /**
   * Checks if this level is at least as severe as the given level.
   *
   * @param other the level to compare against
   * @return true if this level is equal to or more severe than the other level
   */
  public boolean isAtLeast(QAPLogLevel other) {
    return this.severity >= other.severity;
  }
}
