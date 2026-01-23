package com.mk.fx.qa.qap.logging.log4j2;

import com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig;
import com.mk.fx.qa.qap.logging.core.QAPLogEntry;
import com.mk.fx.qa.qap.logging.core.QAPLogLevel;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Log4j2 appender that captures log events in a thread-safe manner. Uses ThreadLocal storage
 * for parallel test execution.
 *
 * <p>This appender is dynamically attached to the root logger when log capture starts and detached
 * when it stops.
 */
@Plugin(
    name = "QAPLog4j2Appender",
    category = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE)
public class QAPLog4j2Appender extends AbstractAppender {

  private static final Logger log = LoggerFactory.getLogger(QAPLog4j2Appender.class);

  // Thread-local storage for log buffers
  private final ThreadLocal<Map<String, List<QAPLogEntry>>> threadLocalBuffers =
      new ThreadLocal<>();

  // Global registry of all active test captures (testId -> config)
  private final Map<String, QAPLogCaptureConfig> activeCaptures = new ConcurrentHashMap<>();

  protected QAPLog4j2Appender(String name, Filter filter, boolean ignoreExceptions) {
    super(name, filter, null, ignoreExceptions, Property.EMPTY_ARRAY);
  }

  @PluginFactory
  public static QAPLog4j2Appender createAppender(
      @PluginAttribute("name") String name,
      @PluginElement("Filter") Filter filter,
      @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true)
          boolean ignoreExceptions) {
    if (name == null) {
      LOGGER.error("No name provided for QAPLog4j2Appender");
      return null;
    }
    return new QAPLog4j2Appender(name, filter, ignoreExceptions);
  }

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
  public void append(LogEvent event) {
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
  private boolean shouldCapture(LogEvent event, QAPLogCaptureConfig config) {
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
   * Converts a Log4j2 LogEvent to QAPLogEntry.
   *
   * @param event the log event
   * @param config capture configuration
   * @return QAPLogEntry
   */
  private QAPLogEntry convertLogEvent(LogEvent event, QAPLogCaptureConfig config) {
    QAPLogEntry.Builder builder = QAPLogEntry.builder();

    // Timestamp
    builder.timestamp(Instant.ofEpochMilli(event.getTimeMillis()));

    // Level
    builder.level(convertLevel(event.getLevel()));

    // Logger name
    builder.loggerName(event.getLoggerName());

    // Thread name
    builder.threadName(event.getThreadName());

    // Message
    Message message = event.getMessage();
    String messageStr = message != null ? message.getFormattedMessage() : null;
    if (messageStr != null && messageStr.length() > config.getMaxMessageLength()) {
      messageStr = messageStr.substring(0, config.getMaxMessageLength()) + "... [truncated]";
    }
    builder.message(messageStr);

    // Throwable
    Throwable throwable = event.getThrown();
    if (throwable != null && config.isCaptureStackTraces()) {
      builder.throwableMessage(throwable.toString());

      // Stack trace
      StackTraceElement[] stackTrace = throwable.getStackTrace();
      if (stackTrace != null && stackTrace.length > 0) {
        String[] stackTraceLines = new String[Math.min(stackTrace.length, 50)]; // Limit to 50 lines
        for (int i = 0; i < stackTraceLines.length; i++) {
          stackTraceLines[i] = stackTrace[i].toString();
        }
        builder.stackTrace(stackTraceLines);
      }
    }

    // MDC (ThreadContext in Log4j2)
    if (config.isIncludeMdc()) {
      Map<String, String> contextMap = event.getContextData().toMap();
      if (contextMap != null && !contextMap.isEmpty()) {
        builder.mdc(contextMap);
      }
    }

    // Markers
    if (config.isIncludeMarkers()) {
      org.apache.logging.log4j.Marker marker = event.getMarker();
      if (marker != null) {
        Set<String> markers = new HashSet<>();
        collectMarkers(marker, markers);
        if (!markers.isEmpty()) {
          builder.markers(markers);
        }
      }
    }

    return builder.build();
  }

  /**
   * Recursively collects all markers (including parents).
   *
   * @param marker the marker
   * @param result set to collect marker names
   */
  private void collectMarkers(org.apache.logging.log4j.Marker marker, Set<String> result) {
    if (marker == null) {
      return;
    }
    result.add(marker.getName());
    if (marker.hasParents()) {
      org.apache.logging.log4j.Marker[] parents = marker.getParents();
      for (org.apache.logging.log4j.Marker parent : parents) {
        collectMarkers(parent, result);
      }
    }
  }

  /**
   * Converts Log4j2 Level to QAPLogLevel.
   *
   * @param level Log4j2 level
   * @return QAPLogLevel
   */
  private QAPLogLevel convertLevel(org.apache.logging.log4j.Level level) {
    if (level == null) {
      return QAPLogLevel.INFO;
    }

    switch (level.getStandardLevel()) {
      case TRACE:
        return QAPLogLevel.TRACE;
      case DEBUG:
        return QAPLogLevel.DEBUG;
      case INFO:
        return QAPLogLevel.INFO;
      case WARN:
        return QAPLogLevel.WARN;
      case ERROR:
        return QAPLogLevel.ERROR;
      case FATAL:
        return QAPLogLevel.FATAL;
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
