package com.mk.fx.qa.qap.junit.logging;

import com.mk.fx.qa.qap.junit.model.QAPPropertiesLoader;
import com.mk.fx.qa.qap.junit.runtime.QAPRuntime;
import com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig;
import com.mk.fx.qa.qap.logging.core.QAPLogCapturer;
import com.mk.fx.qa.qap.logging.core.QAPLogCapturerRegistry;
import com.mk.fx.qa.qap.logging.core.QAPLogEntry;
import com.mk.fx.qa.qap.logging.core.QAPLogLevel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages log capture lifecycle for QAP test execution. Handles initialization, starting/stopping
 * capture, and configuration.
 */
public class QAPLogCaptureManager {

  private static final Logger log = LoggerFactory.getLogger(QAPLogCaptureManager.class);

  private final QAPRuntime runtime;
  private final AtomicReference<QAPLogCapturerRegistry> registry = new AtomicReference<>();
  private final AtomicReference<QAPLogCapturer> capturer = new AtomicReference<>();
  private final AtomicReference<QAPLogCaptureConfig> config = new AtomicReference<>();

  public QAPLogCaptureManager(QAPRuntime runtime) {
    this.runtime = Objects.requireNonNull(runtime, "runtime");
  }

  /**
   * Initializes log capture by discovering available capturers. Should be called once during
   * extension initialization.
   */
  public void initialize() {
    try {
      QAPLogCapturerRegistry logRegistry = new QAPLogCapturerRegistry();
      logRegistry.discover();
      registry.set(logRegistry);

      Optional<QAPLogCapturer> capturerOpt = logRegistry.getAvailableCapturer();

      if (capturerOpt.isPresent()) {
        QAPLogCapturer logCapturer = capturerOpt.get();
        capturer.set(logCapturer);

        // Build configuration from properties
        QAPLogCaptureConfig logConfig = buildConfig();
        config.set(logConfig);

        log.info(
            "✅ Log capture enabled: {} (priority: {})",
            logCapturer.getFrameworkName(),
            logCapturer.getPriority());
      } else {
        log.debug("No log capturer available - tests will run without log capture");
      }
    } catch (Exception e) {
      log.warn("Failed to initialize log capture: {}", e.getMessage());
      capturer.set(null);
    }
  }

  /**
   * Checks if log capture is available and enabled.
   *
   * @return true if a capturer is available
   */
  public boolean isAvailable() {
    return capturer.get() != null;
  }

  /**
   * Gets the current log capturer, if available.
   *
   * @return Optional containing the capturer, or empty if not available
   */
  public Optional<QAPLogCapturer> getCapturer() {
    return Optional.ofNullable(capturer.get());
  }

  /**
   * Gets the current log capture configuration.
   *
   * @return Optional containing the config, or empty if not available
   */
  public Optional<QAPLogCaptureConfig> getConfig() {
    return Optional.ofNullable(config.get());
  }

  /**
   * Starts log capture for a specific test identifier.
   *
   * @param testId unique identifier for the test
   */
  public void startCapture(String testId) {
    QAPLogCapturer logCapturer = capturer.get();
    QAPLogCaptureConfig logConfig = config.get();

    if (logCapturer == null || logConfig == null) {
      return;
    }

    try {
      logCapturer.startCapture(testId, logConfig);
      log.debug("Started log capture for: {}", testId);
    } catch (Exception e) {
      log.warn("Failed to start log capture for {}: {}", testId, e.getMessage());
    }
  }

  /**
   * Stops log capture and returns captured entries for a test identifier.
   *
   * @param testId unique identifier for the test
   * @return list of captured log entries (empty if capture failed or unavailable)
   */
  public List<QAPLogEntry> stopCaptureAndGet(String testId) {
    QAPLogCapturer logCapturer = capturer.get();

    if (logCapturer == null) {
      return List.of();
    }

    try {
      List<QAPLogEntry> entries = logCapturer.stopCapture(testId);
      log.debug("Stopped log capture for: {} ({} entries)", testId, entries.size());
      return entries;
    } catch (Exception e) {
      log.warn("Failed to stop log capture for {}: {}", testId, e.getMessage());
      return List.of();
    }
  }

  /**
   * Builds log capture configuration from QAP properties.
   *
   * @return configured QAPLogCaptureConfig
   */
  private QAPLogCaptureConfig buildConfig() {
    QAPPropertiesLoader props = runtime.getPropertiesLoader();

    QAPLogCaptureConfig.Builder builder =
        QAPLogCaptureConfig.builder()
            .enabled(props.isLoggingEnabled())
            .maxEntriesPerTest(props.getLoggingMaxEntries())
            .maxMessageLength(props.getLoggingMaxMessageLength())
            .captureStackTraces(props.isLoggingCaptureStackTraces())
            .includeMdc(props.isLoggingIncludeMdc())
            .includeMarkers(props.isLoggingIncludeMarkers());

    // Parse min level
    String minLevelStr = props.getLoggingMinLevel();
    try {
      QAPLogLevel minLevel = QAPLogLevel.valueOf(minLevelStr.toUpperCase());
      builder.minLevel(minLevel);
    } catch (IllegalArgumentException e) {
      log.warn(
          "Invalid qap.logging.min.level '{}', using DEBUG. Valid values: TRACE, DEBUG, INFO, WARN, ERROR, FATAL",
          minLevelStr);
      builder.minLevel(QAPLogLevel.DEBUG);
    }

    // Parse logger patterns
    String patternsStr = props.getLoggingLoggerPatterns();
    if (patternsStr != null && !patternsStr.trim().isEmpty()) {
      String[] patterns = patternsStr.split(",");
      for (String pattern : patterns) {
        String trimmed = pattern.trim();
        if (!trimmed.isEmpty()) {
          builder.addLoggerPattern(trimmed);
        }
      }
    }

    return builder.build();
  }
}
