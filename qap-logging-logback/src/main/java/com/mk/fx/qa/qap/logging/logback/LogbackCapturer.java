package com.mk.fx.qa.qap.logging.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig;
import com.mk.fx.qa.qap.logging.core.QAPLogCapturer;
import com.mk.fx.qa.qap.logging.core.QAPLogEntry;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.LoggerFactory;

/**
 * Logback implementation of QAPLogCapturer. Captures logs from Logback by attaching a custom
 * appender to the root logger.
 *
 * <p>Thread-safe and designed for parallel test execution using ThreadLocal storage in the
 * appender.
 */
public class LogbackCapturer implements QAPLogCapturer {

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(LogbackCapturer.class);
  private static final String APPENDER_NAME = "QAPLogbackAppender";

  private volatile QAPLogbackAppender appender;
  private volatile boolean initialized = false;

  @Override
  public void startCapture(String testId, QAPLogCaptureConfig config) {
    Objects.requireNonNull(testId, "testId cannot be null");
    Objects.requireNonNull(config, "config cannot be null");

    if (!config.isEnabled()) {
      log.debug("Log capture disabled for test: {}", testId);
      return;
    }

    ensureInitialized();

    if (appender != null) {
      appender.startCapture(testId, config);
      log.debug("Started Logback capture for test: {}", testId);
    } else {
      log.warn("Failed to start capture - appender not initialized");
    }
  }

  @Override
  public List<QAPLogEntry> stopCapture(String testId) {
    Objects.requireNonNull(testId, "testId cannot be null");

    if (appender == null) {
      log.warn("Appender not initialized, returning empty log list for test: {}", testId);
      return Collections.emptyList();
    }

    List<QAPLogEntry> logs = appender.stopCapture(testId);
    log.debug("Stopped Logback capture for test: {} ({} entries)", testId, logs.size());
    return logs;
  }

  @Override
  public String getFrameworkName() {
    return "Logback";
  }

  @Override
  public boolean isAvailable() {
    try {
      // Check if Logback classes are on the classpath
      Class.forName("ch.qos.logback.classic.LoggerContext");
      Class.forName("ch.qos.logback.core.AppenderBase");

      // Try to get the LoggerContext
      org.slf4j.ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (!(factory instanceof LoggerContext)) {
        log.debug("ILoggerFactory is not a Logback LoggerContext: {}", factory.getClass());
        return false;
      }

      log.debug("Logback is available and ready");
      return true;
    } catch (ClassNotFoundException e) {
      log.debug("Logback classes not found on classpath: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      log.warn("Error checking Logback availability: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public int getPriority() {
    return 0; // Default priority (Log4j2 has priority 100)
  }

  @Override
  public void shutdown() {
    if (appender != null) {
      try {
        appender.cleanupThreadLocals();

        // Remove appender from root logger
        org.slf4j.ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (factory instanceof LoggerContext) {
          LoggerContext context = (LoggerContext) factory;
          Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
          rootLogger.detachAppender(appender);
          appender.stop();

          log.info("QAP Logback appender removed and stopped");
        }
      } catch (Exception e) {
        log.warn("Error during Logback capturer shutdown", e);
      } finally {
        appender = null;
        initialized = false;
      }
    }
  }

  /**
   * Initializes the appender and attaches it to the root logger. This is done lazily on the first
   * capture request.
   */
  private synchronized void ensureInitialized() {
    if (initialized) {
      return;
    }

    try {
      org.slf4j.ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (!(factory instanceof LoggerContext)) {
        throw new IllegalStateException(
            "ILoggerFactory is not a Logback LoggerContext: " + factory.getClass());
      }

      LoggerContext context = (LoggerContext) factory;
      Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

      // Check if appender already exists (shouldn't happen, but be defensive)
      ch.qos.logback.core.Appender<?> existingAppender = rootLogger.getAppender(APPENDER_NAME);
      if (existingAppender instanceof QAPLogbackAppender) {
        log.debug("QAP appender already exists, reusing");
        appender = (QAPLogbackAppender) existingAppender;
      } else {
        // Create and start the appender
        appender = new QAPLogbackAppender();
        appender.setName(APPENDER_NAME);
        appender.setContext(context);
        appender.start();

        // Add to root logger
        rootLogger.addAppender(appender);

        log.info("QAP Logback appender attached to root logger");
      }

      initialized = true;
    } catch (Exception e) {
      log.error("Failed to initialize Logback capturer", e);
      throw new RuntimeException("Failed to initialize Logback capturer", e);
    }
  }

  /**
   * For testing: checks if the appender has any active captures.
   *
   * @return true if at least one test is actively capturing logs
   */
  boolean hasActiveCaptures() {
    return appender != null && appender.hasActiveCaptures();
  }
}
