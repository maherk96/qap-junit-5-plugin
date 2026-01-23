package com.mk.fx.qa.qap.logging.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class QAPLogLevelTest {

  @Test
  void testSeverityOrdering() {
    assertTrue(QAPLogLevel.TRACE.getSeverity() < QAPLogLevel.DEBUG.getSeverity());
    assertTrue(QAPLogLevel.DEBUG.getSeverity() < QAPLogLevel.INFO.getSeverity());
    assertTrue(QAPLogLevel.INFO.getSeverity() < QAPLogLevel.WARN.getSeverity());
    assertTrue(QAPLogLevel.WARN.getSeverity() < QAPLogLevel.ERROR.getSeverity());
    assertTrue(QAPLogLevel.ERROR.getSeverity() < QAPLogLevel.FATAL.getSeverity());
  }

  @Test
  void testIsAtLeast() {
    assertTrue(QAPLogLevel.ERROR.isAtLeast(QAPLogLevel.INFO));
    assertTrue(QAPLogLevel.ERROR.isAtLeast(QAPLogLevel.ERROR));
    assertFalse(QAPLogLevel.INFO.isAtLeast(QAPLogLevel.ERROR));

    assertTrue(QAPLogLevel.WARN.isAtLeast(QAPLogLevel.DEBUG));
    assertFalse(QAPLogLevel.DEBUG.isAtLeast(QAPLogLevel.WARN));
  }

  @Test
  void testAllLevelsAtLeastTrace() {
    for (QAPLogLevel level : QAPLogLevel.values()) {
      assertTrue(level.isAtLeast(QAPLogLevel.TRACE));
    }
  }

  @Test
  void testOnlyFatalAtLeastFatal() {
    assertTrue(QAPLogLevel.FATAL.isAtLeast(QAPLogLevel.FATAL));
    assertFalse(QAPLogLevel.ERROR.isAtLeast(QAPLogLevel.FATAL));
    assertFalse(QAPLogLevel.WARN.isAtLeast(QAPLogLevel.FATAL));
  }
}
