package com.mk.fx.qa.qap.logging.log4j2;

import static org.junit.jupiter.api.Assertions.*;

import com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig;
import com.mk.fx.qa.qap.logging.core.QAPLogEntry;
import com.mk.fx.qa.qap.logging.core.QAPLogLevel;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Log4j2CapturerTest {

  private static final Logger logger = LogManager.getLogger(Log4j2CapturerTest.class);

  private Log4j2Capturer capturer;

  @BeforeEach
  void setUp() {
    capturer = new Log4j2Capturer();
    ThreadContext.clearAll(); // Clean MDC/ThreadContext
  }

  @AfterEach
  void tearDown() {
    if (capturer != null) {
      capturer.shutdown();
    }
    ThreadContext.clearAll();
  }

  @Test
  void testFrameworkName() {
    assertEquals("Log4j2", capturer.getFrameworkName());
  }

  @Test
  void testIsAvailable() {
    assertTrue(capturer.isAvailable(), "Log4j2 should be available in test environment");
  }

  @Test
  void testPriority() {
    assertEquals(100, capturer.getPriority(), "Log4j2 should have priority 100");
  }

  @Test
  void testBasicLogCapture() {
    String testId = "test-basic-capture";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.defaultConfig();

    capturer.startCapture(testId, config);

    logger.info("Test info message");
    logger.warn("Test warning message");
    logger.error("Test error message");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertNotNull(logs);
    assertEquals(3, logs.size());

    QAPLogEntry infoLog = logs.get(0);
    assertEquals(QAPLogLevel.INFO, infoLog.getLevel());
    assertEquals("Test info message", infoLog.getMessage());
    assertEquals(Log4j2CapturerTest.class.getName(), infoLog.getLoggerName());

    QAPLogEntry warnLog = logs.get(1);
    assertEquals(QAPLogLevel.WARN, warnLog.getLevel());
    assertEquals("Test warning message", warnLog.getMessage());

    QAPLogEntry errorLog = logs.get(2);
    assertEquals(QAPLogLevel.ERROR, errorLog.getLevel());
    assertEquals("Test error message", errorLog.getMessage());
  }

  @Test
  void testMinLevelFiltering() {
    String testId = "test-level-filtering";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().minLevel(QAPLogLevel.WARN).build();

    capturer.startCapture(testId, config);

    logger.trace("Trace - should be filtered");
    logger.debug("Debug - should be filtered");
    logger.info("Info - should be filtered");
    logger.warn("Warn - should be captured");
    logger.error("Error - should be captured");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(2, logs.size());
    assertEquals(QAPLogLevel.WARN, logs.get(0).getLevel());
    assertEquals(QAPLogLevel.ERROR, logs.get(1).getLevel());
  }

  @Test
  void testLoggerPatternFiltering() {
    String testId = "test-pattern-filtering";
    QAPLogCaptureConfig config =
        QAPLogCaptureConfig.builder().addLoggerPattern("com.mk.fx.qa.qap.logging.log4j2.*").build();

    capturer.startCapture(testId, config);

    // This should be captured (matches pattern)
    logger.info("Should be captured");

    // This would not be captured if we logged from a different logger
    Logger otherLogger = LogManager.getLogger("org.example.OtherClass");
    otherLogger.info("Should NOT be captured");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertEquals("Should be captured", logs.get(0).getMessage());
  }

  @Test
  void testMdcCapture() {
    String testId = "test-mdc-capture";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().includeMdc(true).build();

    capturer.startCapture(testId, config);

    ThreadContext.put("requestId", "12345");
    ThreadContext.put("userId", "user@example.com");

    logger.info("Message with MDC");

    ThreadContext.clearAll();

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getMdc());
    assertEquals("12345", logs.get(0).getMdc().get("requestId"));
    assertEquals("user@example.com", logs.get(0).getMdc().get("userId"));
  }

  @Test
  void testMarkerCapture() {
    String testId = "test-marker-capture";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().includeMarkers(true).build();

    capturer.startCapture(testId, config);

    Marker importantMarker = MarkerManager.getMarker("IMPORTANT");
    logger.info(importantMarker, "Important message");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getMarkers());
    assertTrue(logs.get(0).getMarkers().contains("IMPORTANT"));
  }

  @Test
  void testExceptionCapture() {
    String testId = "test-exception-capture";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().captureStackTraces(true).build();

    capturer.startCapture(testId, config);

    Exception testException = new RuntimeException("Test exception");
    logger.error("Error with exception", testException);

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    QAPLogEntry logEntry = logs.get(0);
    assertNotNull(logEntry.getThrowableMessage());
    assertTrue(logEntry.getThrowableMessage().contains("Test exception"));
    assertNotNull(logEntry.getStackTrace());
    assertTrue(logEntry.getStackTrace().length > 0);
  }

  @Test
  void testMaxEntriesLimit() {
    String testId = "test-max-entries";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().maxEntriesPerTest(5).build();

    capturer.startCapture(testId, config);

    for (int i = 0; i < 10; i++) {
      logger.info("Message " + i);
    }

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    // Should only capture first 5 messages
    assertEquals(5, logs.size());
    assertEquals("Message 0", logs.get(0).getMessage());
    assertEquals("Message 4", logs.get(4).getMessage());
  }

  @Test
  void testMessageTruncation() {
    String testId = "test-message-truncation";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().maxMessageLength(50).build();

    capturer.startCapture(testId, config);

    String longMessage = "A".repeat(100); // 100 character message
    logger.info(longMessage);

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    String capturedMessage = logs.get(0).getMessage();
    // Message should either be truncated (shorter than original) or contain truncation marker
    assertTrue(
        capturedMessage.length() < 100 || capturedMessage.contains("truncated"),
        "Message should be truncated. Actual length: "
            + capturedMessage.length()
            + ", message: "
            + capturedMessage.substring(0, Math.min(60, capturedMessage.length())));
    if (capturedMessage.contains("truncated")) {
      assertTrue(capturedMessage.endsWith("... [truncated]"));
    }
  }

  @Test
  void testDisabledCapture() {
    String testId = "test-disabled";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().enabled(false).build();

    capturer.startCapture(testId, config);

    logger.info("This should not be captured");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertTrue(logs.isEmpty());
  }

  @Test
  void testMultipleConcurrentCaptures() throws InterruptedException {
    String testId1 = "test-concurrent-1";
    String testId2 = "test-concurrent-2";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.defaultConfig();

    capturer.startCapture(testId1, config);
    capturer.startCapture(testId2, config);

    // Log from main thread
    logger.info("Main thread message");

    // Log from another thread
    Thread otherThread =
        new Thread(
            () -> {
              Logger threadLogger = LogManager.getLogger(Log4j2CapturerTest.class);
              threadLogger.info("Other thread message");
            });
    otherThread.start();
    otherThread.join();

    List<QAPLogEntry> logs1 = capturer.stopCapture(testId1);
    List<QAPLogEntry> logs2 = capturer.stopCapture(testId2);

    // Both should capture the main thread message
    assertFalse(logs1.isEmpty());
    assertFalse(logs2.isEmpty());
  }

  @Test
  void testThreadNameCapture() {
    String testId = "test-thread-name";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.defaultConfig();

    capturer.startCapture(testId, config);

    logger.info("Message from test thread");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getThreadName());
    // Thread name should be something like "Test worker" or similar
    assertFalse(logs.get(0).getThreadName().isEmpty());
  }

  @Test
  void testTimestampCapture() {
    String testId = "test-timestamp";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.defaultConfig();

    capturer.startCapture(testId, config);

    long beforeLog = System.currentTimeMillis();
    logger.info("Timestamped message");
    long afterLog = System.currentTimeMillis();

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getTimestamp());
    long logTime = logs.get(0).getTimestamp().toEpochMilli();
    assertTrue(
        logTime >= beforeLog && logTime <= afterLog, "Timestamp should be within log time range");
  }

  @Test
  void testStopCaptureWithoutStart() {
    String testId = "test-no-start";

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertNotNull(logs);
    assertTrue(logs.isEmpty());
  }

  @Test
  void testMultipleStartStopCycles() {
    String testId = "test-cycles";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.defaultConfig();

    // Cycle 1
    capturer.startCapture(testId, config);
    logger.info("Cycle 1 message");
    List<QAPLogEntry> logs1 = capturer.stopCapture(testId);
    assertEquals(1, logs1.size());

    // Cycle 2
    capturer.startCapture(testId, config);
    logger.info("Cycle 2 message");
    List<QAPLogEntry> logs2 = capturer.stopCapture(testId);
    assertEquals(1, logs2.size());

    // Verify logs are independent
    assertEquals("Cycle 1 message", logs1.get(0).getMessage());
    assertEquals("Cycle 2 message", logs2.get(0).getMessage());
  }
}
