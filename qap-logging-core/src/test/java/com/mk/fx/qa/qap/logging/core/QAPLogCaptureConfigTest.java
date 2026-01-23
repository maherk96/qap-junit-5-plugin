package com.mk.fx.qa.qap.logging.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class QAPLogCaptureConfigTest {

  @Test
  void testDefaultConfig() {
    QAPLogCaptureConfig config = QAPLogCaptureConfig.defaultConfig();

    assertTrue(config.isEnabled());
    assertEquals(QAPLogLevel.INFO, config.getMinLevel());
    assertEquals(1000, config.getMaxEntriesPerTest());
    assertEquals(10_000, config.getMaxMessageLength());
    assertTrue(config.isCaptureStackTraces());
    assertTrue(config.isIncludeMdc());
    assertTrue(config.isIncludeMarkers());
    assertTrue(config.isThreadLocal());
  }

  @Test
  void testBuilder() {
    QAPLogCaptureConfig config =
        QAPLogCaptureConfig.builder()
            .enabled(false)
            .minLevel(QAPLogLevel.DEBUG)
            .maxEntriesPerTest(500)
            .maxMessageLength(5000)
            .captureStackTraces(false)
            .includeMdc(false)
            .includeMarkers(false)
            .threadLocal(false)
            .build();

    assertFalse(config.isEnabled());
    assertEquals(QAPLogLevel.DEBUG, config.getMinLevel());
    assertEquals(500, config.getMaxEntriesPerTest());
    assertEquals(5000, config.getMaxMessageLength());
    assertFalse(config.isCaptureStackTraces());
    assertFalse(config.isIncludeMdc());
    assertFalse(config.isIncludeMarkers());
    assertFalse(config.isThreadLocal());
  }

  @Test
  void testLoggerPatternMatching() {
    QAPLogCaptureConfig config =
        QAPLogCaptureConfig.builder()
            .addLoggerPattern("com.example.*")
            .addLoggerPattern("org.springframework.web.*")
            .build();

    assertTrue(config.matchesLoggerPattern("com.example.MyClass"));
    assertTrue(config.matchesLoggerPattern("com.example.service.MyService"));
    assertTrue(config.matchesLoggerPattern("org.springframework.web.Controller"));
    assertFalse(config.matchesLoggerPattern("org.springframework.boot.Application"));
    assertFalse(config.matchesLoggerPattern("com.other.Class"));
  }

  @Test
  void testLoggerPatternMatchingNoPatterns() {
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().build();

    // No patterns = match all
    assertTrue(config.matchesLoggerPattern("com.example.Test"));
    assertTrue(config.matchesLoggerPattern("anything.goes.Here"));
  }

  @Test
  void testShouldCapture() {
    QAPLogCaptureConfig config = QAPLogCaptureConfig.builder().minLevel(QAPLogLevel.WARN).build();

    assertFalse(config.shouldCapture(QAPLogLevel.DEBUG));
    assertFalse(config.shouldCapture(QAPLogLevel.INFO));
    assertTrue(config.shouldCapture(QAPLogLevel.WARN));
    assertTrue(config.shouldCapture(QAPLogLevel.ERROR));
    assertTrue(config.shouldCapture(QAPLogLevel.FATAL));
  }

  @Test
  void testInvalidMaxEntries() {
    assertThrows(
        IllegalArgumentException.class,
        () -> QAPLogCaptureConfig.builder().maxEntriesPerTest(0).build());

    assertThrows(
        IllegalArgumentException.class,
        () -> QAPLogCaptureConfig.builder().maxEntriesPerTest(-1).build());
  }

  @Test
  void testInvalidMaxMessageLength() {
    assertThrows(
        IllegalArgumentException.class,
        () -> QAPLogCaptureConfig.builder().maxMessageLength(0).build());

    assertThrows(
        IllegalArgumentException.class,
        () -> QAPLogCaptureConfig.builder().maxMessageLength(-1).build());
  }

  @Test
  void testAddMultipleLoggerPatterns() {
    QAPLogCaptureConfig config =
        QAPLogCaptureConfig.builder()
            .addLoggerPatterns(List.of("com.example.*", "org.test.*"))
            .build();

    assertTrue(config.matchesLoggerPattern("com.example.Test"));
    assertTrue(config.matchesLoggerPattern("org.test.Another"));
    assertFalse(config.matchesLoggerPattern("com.other.Class"));
  }

  @Test
  void testWildcardPattern() {
    QAPLogCaptureConfig config =
        QAPLogCaptureConfig.builder().addLoggerPattern("com.*.service.*").build();

    assertTrue(config.matchesLoggerPattern("com.example.service.UserService"));
    assertTrue(config.matchesLoggerPattern("com.test.service.DataService"));
    assertFalse(config.matchesLoggerPattern("com.example.controller.UserController"));
  }

  @Test
  void testNullPatternThrows() {
    assertThrows(
        NullPointerException.class, () -> QAPLogCaptureConfig.builder().addLoggerPattern(null));

    assertThrows(
        NullPointerException.class, () -> QAPLogCaptureConfig.builder().addLoggerPatterns(null));
  }
}
