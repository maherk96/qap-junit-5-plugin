package com.mk.fx.qa.qap.logging.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Configuration for log capture behavior. Immutable and thread-safe. */
public class QAPLogCaptureConfig {

  private static final int DEFAULT_MAX_ENTRIES = 1000;
  private static final int DEFAULT_MAX_MESSAGE_LENGTH = 10_000;

  private final boolean enabled;
  private final QAPLogLevel minLevel;
  private final int maxEntriesPerTest;
  private final int maxMessageLength;
  private final boolean captureStackTraces;
  private final boolean includeMdc;
  private final boolean includeMarkers;
  private final boolean threadLocal;
  private final List<Pattern> loggerPatterns;

  private QAPLogCaptureConfig(Builder builder) {
    this.enabled = builder.enabled;
    this.minLevel = builder.minLevel;
    this.maxEntriesPerTest = builder.maxEntriesPerTest;
    this.maxMessageLength = builder.maxMessageLength;
    this.captureStackTraces = builder.captureStackTraces;
    this.includeMdc = builder.includeMdc;
    this.includeMarkers = builder.includeMarkers;
    this.threadLocal = builder.threadLocal;
    this.loggerPatterns = Collections.unmodifiableList(new ArrayList<>(builder.loggerPatterns));
  }

  public boolean isEnabled() {
    return enabled;
  }

  public QAPLogLevel getMinLevel() {
    return minLevel;
  }

  public int getMaxEntriesPerTest() {
    return maxEntriesPerTest;
  }

  public int getMaxMessageLength() {
    return maxMessageLength;
  }

  public boolean isCaptureStackTraces() {
    return captureStackTraces;
  }

  public boolean isIncludeMdc() {
    return includeMdc;
  }

  public boolean isIncludeMarkers() {
    return includeMarkers;
  }

  public boolean isThreadLocal() {
    return threadLocal;
  }

  public List<Pattern> getLoggerPatterns() {
    return loggerPatterns;
  }

  /**
   * Checks if a logger name matches any of the configured patterns. If no patterns are configured,
   * all loggers match.
   *
   * @param loggerName the logger name to check
   * @return true if the logger should be captured
   */
  public boolean matchesLoggerPattern(String loggerName) {
    if (loggerPatterns.isEmpty()) {
      return true; // No filter = capture all
    }
    return loggerPatterns.stream().anyMatch(pattern -> pattern.matcher(loggerName).matches());
  }

  /**
   * Checks if a log level should be captured based on the minimum level.
   *
   * @param level the log level to check
   * @return true if the level should be captured
   */
  public boolean shouldCapture(QAPLogLevel level) {
    return level.isAtLeast(minLevel);
  }

  /**
   * Creates a default configuration with sensible defaults.
   *
   * @return default configuration
   */
  public static QAPLogCaptureConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Creates a new builder for constructing configuration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for QAPLogCaptureConfig. */
  public static class Builder {
    private boolean enabled = true;
    private QAPLogLevel minLevel = QAPLogLevel.INFO;
    private int maxEntriesPerTest = DEFAULT_MAX_ENTRIES;
    private int maxMessageLength = DEFAULT_MAX_MESSAGE_LENGTH;
    private boolean captureStackTraces = true;
    private boolean includeMdc = true;
    private boolean includeMarkers = true;
    private boolean threadLocal = true;
    private List<Pattern> loggerPatterns = new ArrayList<>();

    /** Enable or disable log capture. Default: true */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /** Set minimum log level to capture. Default: INFO */
    public Builder minLevel(QAPLogLevel minLevel) {
      this.minLevel = Objects.requireNonNull(minLevel, "minLevel cannot be null");
      return this;
    }

    /**
     * Set maximum number of log entries to capture per test. Prevents memory issues with chatty
     * tests. Default: 1000
     */
    public Builder maxEntriesPerTest(int maxEntriesPerTest) {
      if (maxEntriesPerTest <= 0) {
        throw new IllegalArgumentException("maxEntriesPerTest must be positive");
      }
      this.maxEntriesPerTest = maxEntriesPerTest;
      return this;
    }

    /**
     * Set maximum length for log messages. Messages longer than this will be truncated. Default:
     * 10000 characters
     */
    public Builder maxMessageLength(int maxMessageLength) {
      if (maxMessageLength <= 0) {
        throw new IllegalArgumentException("maxMessageLength must be positive");
      }
      this.maxMessageLength = maxMessageLength;
      return this;
    }

    /** Enable or disable stack trace capture for exceptions in logs. Default: true */
    public Builder captureStackTraces(boolean captureStackTraces) {
      this.captureStackTraces = captureStackTraces;
      return this;
    }

    /** Enable or disable MDC (Mapped Diagnostic Context) capture. Default: true */
    public Builder includeMdc(boolean includeMdc) {
      this.includeMdc = includeMdc;
      return this;
    }

    /** Enable or disable marker capture (for structured logging). Default: true */
    public Builder includeMarkers(boolean includeMarkers) {
      this.includeMarkers = includeMarkers;
      return this;
    }

    /**
     * Use ThreadLocal storage for log buffers. Recommended for parallel test execution. Default:
     * true
     */
    public Builder threadLocal(boolean threadLocal) {
      this.threadLocal = threadLocal;
      return this;
    }

    /**
     * Add a logger name pattern to filter which loggers to capture. Supports wildcards:
     * "com.example.*" matches all loggers under com.example. If no patterns are added, all loggers
     * are captured.
     *
     * @param pattern logger name pattern (supports * wildcards)
     */
    public Builder addLoggerPattern(String pattern) {
      Objects.requireNonNull(pattern, "pattern cannot be null");
      // Convert wildcard pattern to regex
      String regex = pattern.replace(".", "\\.").replace("*", ".*");
      this.loggerPatterns.add(Pattern.compile(regex));
      return this;
    }

    /**
     * Add multiple logger name patterns.
     *
     * @param patterns logger name patterns
     */
    public Builder addLoggerPatterns(List<String> patterns) {
      Objects.requireNonNull(patterns, "patterns cannot be null");
      patterns.forEach(this::addLoggerPattern);
      return this;
    }

    public QAPLogCaptureConfig build() {
      return new QAPLogCaptureConfig(this);
    }
  }

  @Override
  public String toString() {
    return "QAPLogCaptureConfig{"
        + "enabled="
        + enabled
        + ", minLevel="
        + minLevel
        + ", maxEntriesPerTest="
        + maxEntriesPerTest
        + ", maxMessageLength="
        + maxMessageLength
        + ", captureStackTraces="
        + captureStackTraces
        + ", includeMdc="
        + includeMdc
        + ", includeMarkers="
        + includeMarkers
        + ", threadLocal="
        + threadLocal
        + ", loggerPatterns="
        + loggerPatterns.size()
        + '}';
  }
}
