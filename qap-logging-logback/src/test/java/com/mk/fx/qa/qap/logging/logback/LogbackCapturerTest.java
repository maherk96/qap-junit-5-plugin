package com.mk.fx.qa.qap.logging.logback;

import static org.junit.jupiter.api.Assertions.*;

import com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig;
import com.mk.fx.qa.qap.logging.core.QAPLogEntry;
import com.mk.fx.qa.qap.logging.core.QAPLogLevel;
import java.util.List;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

class LogbackCapturerTest {

  private static final Logger log = LoggerFactory.getLogger(LogbackCapturerTest.class);
  private LogbackCapturer capturer;

  @BeforeEach
  void setUp() {
    capturer = new LogbackCapturer();
  }

  @AfterEach
  void tearDown() {
    if (capturer != null) {
      capturer.shutdown();
    }
    MDC.clear();
  }

  @Test
  void testIsAvailable() {
    assertTrue(capturer.isAvailable(), "Logback should be available in test environment");
  }

  @Test
  void testGetFrameworkName() {
    assertEquals("Logback", capturer.getFrameworkName());
  }

  @Test
  void testGetPriority() {
    assertEquals(0, capturer.getPriority(), "Logback has default priority 0");
  }

  @Test
  void testBasicLogCapture() {
    String testId = "test-basic-capture";
    capturer.startCapture(testId, QAPLogCaptureConfig.defaultConfig());

    log.info("Test info message");
    log.warn("Test warning message");
    log.error("Test error message");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(3, logs.size());
    assertEquals(QAPLogLevel.INFO, logs.get(0).getLevel());
    assertEquals("Test info message", logs.get(0).getMessage());
    assertEquals(QAPLogLevel.WARN, logs.get(1).getLevel());
    assertEquals("Test warning message", logs.get(1).getMessage());
    assertEquals(QAPLogLevel.ERROR, logs.get(2).getLevel());
    assertEquals("Test error message", logs.get(2).getMessage());
  }

  @Test
  void testLogLevelFiltering() {
    String testId = "test-level-filtering";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().minLevel(QAPLogLevel.WARN).build();

    capturer.startCapture(testId, config);

    log.trace("Should not capture");
    log.debug("Should not capture");
    log.info("Should not capture");
    log.warn("Should capture");
    log.error("Should capture");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(2, logs.size());
    assertEquals(QAPLogLevel.WARN, logs.get(0).getLevel());
    assertEquals(QAPLogLevel.ERROR, logs.get(1).getLevel());
  }

  @Test
  void testDebugLevelCapture() {
    String testId = "test-debug-capture";
    QAPLogCaptureConfig config =
        QAPLogCaptureConfig.builder()
            .minLevel(QAPLogLevel.DEBUG)
            .addLoggerPattern("com.mk.fx.qa.qap.logging.logback.LogbackCapturerTest")
            .build();

    capturer.startCapture(testId, config);

    log.trace("Should not capture");
    log.debug("Should capture");
    log.info("Should capture");
    log.warn("Should capture");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(3, logs.size());
    assertEquals(QAPLogLevel.DEBUG, logs.get(0).getLevel());
    assertEquals("Should capture", logs.get(0).getMessage());
  }

  @Test
  void testLoggerNameFiltering() {
    String testId = "test-logger-filtering";
    QAPLogCaptureConfig config =
        QAPLogCaptureConfig.builder()
            .addLoggerPattern("com.mk.fx.qa.qap.logging.logback.*")
            .build();

    capturer.startCapture(testId, config);

    log.info("Should capture - matches pattern");

    Logger otherLogger = LoggerFactory.getLogger("com.example.Other");
    otherLogger.info("Should not capture - doesn't match pattern");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertEquals("Should capture - matches pattern", logs.get(0).getMessage());
  }

  @Test
  void testMDCCapture() {
    String testId = "test-mdc-capture";
    capturer.startCapture(testId, QAPLogCaptureConfig.defaultConfig());

    MDC.put("requestId", "REQ-12345");
    MDC.put("userId", "user-42");
    log.info("Message with MDC");
    MDC.clear();

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertEquals("Message with MDC", logs.get(0).getMessage());
    assertNotNull(logs.get(0).getMdc());
    assertEquals("REQ-12345", logs.get(0).getMdc().get("requestId"));
    assertEquals("user-42", logs.get(0).getMdc().get("userId"));
  }

  @Test
  void testMarkerCapture() {
    String testId = "test-marker-capture";
    capturer.startCapture(testId, QAPLogCaptureConfig.defaultConfig());

    Marker securityMarker = MarkerFactory.getMarker("SECURITY");
    log.warn(securityMarker, "Security alert");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getMarkers());
    assertTrue(logs.get(0).getMarkers().contains("SECURITY"));
  }

  @Test
  void testExceptionCapture() {
    String testId = "test-exception-capture";
    capturer.startCapture(testId, QAPLogCaptureConfig.defaultConfig());

    Exception exception = new IllegalArgumentException("Invalid argument");
    log.error("Error occurred", exception);

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertEquals("Error occurred", logs.get(0).getMessage());
    assertNotNull(logs.get(0).getThrowableMessage());
    assertTrue(logs.get(0).getThrowableMessage().contains("IllegalArgumentException"));
    assertTrue(logs.get(0).getThrowableMessage().contains("Invalid argument"));
    assertNotNull(logs.get(0).getStackTrace());
    assertTrue(logs.get(0).getStackTrace().length > 0);
  }

  @Test
  void testMaxEntriesLimit() {
    String testId = "test-max-entries";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().maxEntriesPerTest(5).build();

    capturer.startCapture(testId, config);

    for (int i = 0; i < 10; i++) {
      log.info("Message {}", i);
    }

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(5, logs.size(), "Should respect max entries limit");
  }

  @Test
  void testMessageTruncation() {
    String testId = "test-message-truncation";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().maxMessageLength(20).build();

    capturer.startCapture(testId, config);

    String longMessage = "This is a very long message that should be truncated";
    log.info(longMessage);

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertTrue(logs.get(0).getMessage().length() <= 35); // 20 + "... [truncated]"
    assertTrue(logs.get(0).getMessage().endsWith("... [truncated]"));
  }

  @Test
  void testThreadNameCapture() {
    String testId = "test-thread-name";
    capturer.startCapture(testId, QAPLogCaptureConfig.defaultConfig());

    log.info("Test message");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getThreadName());
    assertFalse(logs.get(0).getThreadName().isEmpty());
  }

  @Test
  void testTimestampCapture() {
    String testId = "test-timestamp";
    capturer.startCapture(testId, QAPLogCaptureConfig.defaultConfig());

    log.info("Test message");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getTimestamp());
  }

  @Test
  void testDisabledCapture() {
    String testId = "test-disabled";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().enabled(false).build();

    capturer.startCapture(testId, config);

    log.info("Should not be captured");

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(0, logs.size());
  }

  @Test
  void testMultipleCaptureSessions() {
    String testId1 = "test-1";
    String testId2 = "test-2";

    capturer.startCapture(testId1, QAPLogCaptureConfig.defaultConfig());
    log.info("Message for test 1");
    List<QAPLogEntry> logs1 = capturer.stopCapture(testId1);

    capturer.startCapture(testId2, QAPLogCaptureConfig.defaultConfig());
    log.info("Message for test 2");
    List<QAPLogEntry> logs2 = capturer.stopCapture(testId2);

    assertEquals(1, logs1.size());
    assertEquals("Message for test 1", logs1.get(0).getMessage());

    assertEquals(1, logs2.size());
    assertEquals("Message for test 2", logs2.get(0).getMessage());
  }

  @Test
  void testStopCaptureWithoutStart() {
    String testId = "test-no-start";
    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertNotNull(logs);
    assertEquals(0, logs.size());
  }

  @Test
  void testHasActiveCaptures() {
    assertFalse(capturer.hasActiveCaptures());

    String testId = "test-active";
    capturer.startCapture(testId, QAPLogCaptureConfig.defaultConfig());

    assertTrue(capturer.hasActiveCaptures());

    capturer.stopCapture(testId);

    assertFalse(capturer.hasActiveCaptures());
  }

  @Test
  void testSkipMDCCapture() {
    String testId = "test-skip-mdc";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().includeMdc(false).build();

    capturer.startCapture(testId, config);

    MDC.put("requestId", "REQ-12345");
    log.info("Message with MDC");
    MDC.clear();

    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    assertEquals(1, logs.size());
    assertTrue(logs.get(0).getMdc() == null || logs.get(0).getMdc().isEmpty());
  }
}
