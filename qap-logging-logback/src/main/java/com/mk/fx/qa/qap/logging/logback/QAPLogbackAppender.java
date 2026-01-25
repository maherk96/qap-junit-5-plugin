package com.mk.fx.qa.qap.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig;
import com.mk.fx.qa.qap.logging.core.QAPLogEntry;
import com.mk.fx.qa.qap.logging.core.QAPLogLevel;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Logback appender that captures log events in a thread-safe manner. Uses ThreadLocal
 * storage for parallel test execution.
 *
 * <p>This appender is dynamically attached to the root logger when log capture starts and detached
 * when it stops.
 */
public class QAPLogbackAppender extends AppenderBase<ILoggingEvent> {

  private static final Logger log = LoggerFactory.getLogger(QAPLogbackAppender.class);

  // Thread-local storage for log buffers
  private final ThreadLocal<Map<String, List<QAPLogEntry>>> threadLocalBuffers =
      new ThreadLocal<>();

  // Global registry of all active test captures (testId -> config)
  private final Map<String, QAPLogCaptureConfig> activeCaptures = new ConcurrentHashMap<>();

  /**
   * Starts capturing logs for a specific test.
   *
   * @param testId unique test identifier
   * @param config capture configuration
   */
  public void startCapture(String testId, QAPLogCaptureConfig config) {
    Objects.requireNonNull(testId, "testId cannot be null");
    Objects.requireNonNull(config, "config cannot be null");

    if (!config.isEnabled()) {
      log.debug("Log capture disabled for test: {}", testId);
      return;
    }

    activeCaptures.put(testId, config);

    if (config.isThreadLocal()) {
      Map<String, List<QAPLogEntry>> buffers = threadLocalBuffers.get();
      if (buffers == null) {
        buffers = new HashMap<>();
        threadLocalBuffers.set(buffers);
      }
      buffers.put(testId, new ArrayList<>());
      log.debug("Started ThreadLocal log capture for test: {}", testId);
    } else {
      log.debug("Started global log capture for test: {}", testId);
    }
  }

  /**
   * Stops capturing logs and returns the captured entries.
   *
   * @param testId unique test identifier
   * @return list of captured log entries, never null
   */
  public List<QAPLogEntry> stopCapture(String testId) {
    Objects.requireNonNull(testId, "testId cannot be null");

    QAPLogCaptureConfig config = activeCaptures.remove(testId);
    if (config == null) {
      log.warn("No active capture found for test: {}", testId);
      return Collections.emptyList();
    }

    if (config.isThreadLocal()) {
      Map<String, List<QAPLogEntry>> buffers = threadLocalBuffers.get();
      if (buffers != null) {
        List<QAPLogEntry> logs = buffers.remove(testId);
        if (buffers.isEmpty()) {
          threadLocalBuffers.remove(); // Clean up ThreadLocal
        }
        log.debug(
            "Stopped ThreadLocal log capture for test: {} ({} entries)",
            testId,
            logs != null ? logs.size() : 0);
        return logs != null ? logs : Collections.emptyList();
      }
    }

    log.debug("Stopped log capture for test: {}", testId);
    return Collections.emptyList();
  }

  @Override
  protected void append(ILoggingEvent event) {
    if (activeCaptures.isEmpty()) {
      return; // No active captures, skip processing
    }

    try {
      // Try to capture for all active tests
      for (Map.Entry<String, QAPLogCaptureConfig> entry : activeCaptures.entrySet()) {
        String testId = entry.getKey();
        QAPLogCaptureConfig config = entry.getValue();

        if (shouldCapture(event, config)) {
          QAPLogEntry logEntry = convertLogEvent(event, config);
          addLogEntry(testId, logEntry, config);
        }
      }
    } catch (Exception e) {
      // Never throw exceptions from appender - could break application logging
      log.error("Error capturing log event", e);
    }
  }

  /**
   * Checks if a log event should be captured based on configuration.
   *
   * @param event the log event
   * @param config capture configuration
   * @return true if event should be captured
   */
  private boolean shouldCapture(ILoggingEvent event, QAPLogCaptureConfig config) {
    // Check log level
    QAPLogLevel qapLevel = convertLevel(event.getLevel());
    if (!config.shouldCapture(qapLevel)) {
      return false;
    }

    // Check logger name pattern
    String loggerName = event.getLoggerName();
    if (!config.matchesLoggerPattern(loggerName)) {
      return false;
    }

    return true;
  }

  /**
   * Converts a Logback ILoggingEvent to QAPLogEntry.
   *
   * @param event the log event
   * @param config capture configuration
   * @return QAPLogEntry
   */
  private QAPLogEntry convertLogEvent(ILoggingEvent event, QAPLogCaptureConfig config) {
    QAPLogEntry.Builder builder = QAPLogEntry.builder();

    // Timestamp
    builder.timestamp(Instant.ofEpochMilli(event.getTimeStamp()));

    // Level
    builder.level(convertLevel(event.getLevel()));

    // Logger name
    builder.loggerName(event.getLoggerName());

    // Thread name
    builder.threadName(event.getThreadName());

    // Message
    String messageStr = event.getFormattedMessage();
    if (messageStr != null && messageStr.length() > config.getMaxMessageLength()) {
      messageStr = messageStr.substring(0, config.getMaxMessageLength()) + "... [truncated]";
    }
    builder.message(messageStr);

    // Throwable
    IThrowableProxy throwableProxy = event.getThrowableProxy();
    if (throwableProxy != null && config.isCaptureStackTraces()) {
      builder.throwableMessage(throwableProxy.getClassName() + ": " + throwableProxy.getMessage());

      // Stack trace
      StackTraceElementProxy[] stackTrace = throwableProxy.getStackTraceElementProxyArray();
      if (stackTrace != null && stackTrace.length > 0) {
        String[] stackTraceLines = new String[Math.min(stackTrace.length, 50)]; // Limit to 50 lines
        for (int i = 0; i < stackTraceLines.length; i++) {
          stackTraceLines[i] = stackTrace[i].getStackTraceElement().toString();
        }
        builder.stackTrace(stackTraceLines);
      }
    }

    // MDC
    if (config.isIncludeMdc()) {
      Map<String, String> mdcMap = event.getMDCPropertyMap();
      if (mdcMap != null && !mdcMap.isEmpty()) {
        builder.mdc(mdcMap);
      }
    }

    // Markers
    if (config.isIncludeMarkers()) {
      List<org.slf4j.Marker> markers = event.getMarkerList();
      if (markers != null && !markers.isEmpty()) {
        Set<String> markerNames = new HashSet<>();
        for (org.slf4j.Marker marker : markers) {
          collectMarkers(marker, markerNames);
        }
        if (!markerNames.isEmpty()) {
          builder.markers(markerNames);
        }
      }
    }

    return builder.build();
  }

  /**
   * Recursively collects all markers (including references).
   *
   * @param marker the marker
   * @param result set to collect marker names
   */
  private void collectMarkers(org.slf4j.Marker marker, Set<String> result) {
    if (marker == null) {
      return;
    }
    result.add(marker.getName());
    if (marker.hasReferences()) {
      Iterator<org.slf4j.Marker> it = marker.iterator();
      while (it.hasNext()) {
        collectMarkers(it.next(), result);
      }
    }
  }

  /**
   * Converts Logback Level to QAPLogLevel.
   *
   * @param level Logback level
   * @return QAPLogLevel
   */
  private QAPLogLevel convertLevel(Level level) {
    if (level == null) {
      return QAPLogLevel.INFO;
    }

    switch (level.levelInt) {
      case Level.TRACE_INT:
        return QAPLogLevel.TRACE;
      case Level.DEBUG_INT:
        return QAPLogLevel.DEBUG;
      case Level.INFO_INT:
        return QAPLogLevel.INFO;
      case Level.WARN_INT:
        return QAPLogLevel.WARN;
      case Level.ERROR_INT:
        return QAPLogLevel.ERROR;
      default:
        return QAPLogLevel.INFO;
    }
  }

  /**
   * Adds a log entry to the buffer for a specific test.
   *
   * @param testId test identifier
   * @param logEntry log entry to add
   * @param config capture configuration
   */
  private void addLogEntry(String testId, QAPLogEntry logEntry, QAPLogCaptureConfig config) {
    if (config.isThreadLocal()) {
      Map<String, List<QAPLogEntry>> buffers = threadLocalBuffers.get();
      if (buffers != null) {
        List<QAPLogEntry> logs = buffers.get(testId);
        if (logs != null) {
          if (logs.size() < config.getMaxEntriesPerTest()) {
            logs.add(logEntry);
          } else if (logs.size() == config.getMaxEntriesPerTest()) {
            // Add a warning message that we've hit the limit
            log.warn(
                "Max log entries ({}) reached for test: {}", config.getMaxEntriesPerTest(), testId);
          }
        }
      }
    }
  }

  /**
   * Checks if there are any active captures.
   *
   * @return true if at least one test is actively capturing logs
   */
  public boolean hasActiveCaptures() {
    return !activeCaptures.isEmpty();
  }

  /** Cleans up all ThreadLocal storage. Should be called when the appender is stopped. */
  public void cleanupThreadLocals() {
    threadLocalBuffers.remove();
    activeCaptures.clear();
    log.debug("Cleaned up ThreadLocal storage");
  }
}
