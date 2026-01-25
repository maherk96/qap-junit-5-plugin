package com.mk.fx.qa.qap.logging.log4j2;

import com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig;
import com.mk.fx.qa.qap.logging.core.QAPLogCapturer;
import com.mk.fx.qa.qap.logging.core.QAPLogEntry;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.LoggerFactory;

/**
 * Log4j2 implementation of QAPLogCapturer. Captures logs from Log4j2 by attaching a custom appender
 * to the root logger.
 *
 * <p>Thread-safe and designed for parallel test execution using ThreadLocal storage in the
 * appender.
 */
public class Log4j2Capturer implements QAPLogCapturer {

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(Log4j2Capturer.class);
  private static final String APPENDER_NAME = "QAPLog4j2Appender";

  private volatile QAPLog4j2Appender appender;
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
      log.debug("Started Log4j2 capture for test: {}", testId);
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
    log.debug("Stopped Log4j2 capture for test: {} ({} entries)", testId, logs.size());
    return logs;
  }

  @Override
  public String getFrameworkName() {
    return "Log4j2";
  }

  @Override
  public boolean isAvailable() {
    try {
      // Check if Log4j2 classes are on the classpath
      Class.forName("org.apache.logging.log4j.core.LoggerContext");
      Class.forName("org.apache.logging.log4j.core.appender.AbstractAppender");

      // Try to get the LoggerContext
      org.apache.logging.log4j.core.LoggerContext context =
          (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
      if (context == null) {
        log.debug("Log4j2 LoggerContext is null");
        return false;
      }

      log.debug("Log4j2 is available and ready");
      return true;
    } catch (ClassNotFoundException e) {
      log.debug("Log4j2 classes not found on classpath: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      log.warn("Error checking Log4j2 availability: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public int getPriority() {
    return 100; // Higher priority than Logback (default 0)
  }

  @Override
  public void shutdown() {
    if (appender != null) {
      try {
        appender.cleanupThreadLocals();

        // Remove appender from root logger
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Logger rootLogger = context.getRootLogger();
        rootLogger.removeAppender(appender);
        appender.stop();

        log.info("QAP Log4j2 appender removed and stopped");
      } catch (Exception e) {
        log.warn("Error during Log4j2 capturer shutdown", e);
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
      LoggerContext context = (LoggerContext) LogManager.getContext(false);
      Logger rootLogger = context.getRootLogger();

      // Check if appender already exists (shouldn't happen, but be defensive)
      if (rootLogger.getAppenders().containsKey(APPENDER_NAME)) {
        log.debug("QAP appender already exists, reusing");
        appender = (QAPLog4j2Appender) rootLogger.getAppenders().get(APPENDER_NAME);
      } else {
        // Create and start the appender
        appender = QAPLog4j2Appender.createAppender(APPENDER_NAME, null, true);
        if (appender == null) {
          throw new IllegalStateException("Failed to create QAPLog4j2Appender");
        }

        appender.start();

        // Add to root logger
        rootLogger.addAppender(appender);

        log.info("QAP Log4j2 appender attached to root logger");
      }

      initialized = true;
    } catch (Exception e) {
      log.error("Failed to initialize Log4j2 capturer", e);
      throw new RuntimeException("Failed to initialize Log4j2 capturer", e);
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
