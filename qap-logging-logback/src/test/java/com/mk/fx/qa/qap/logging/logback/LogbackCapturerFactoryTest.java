package com.mk.fx.qa.qap.logging.logback;

import static org.junit.jupiter.api.Assertions.*;

import com.mk.fx.qa.qap.logging.core.QAPLogCapturer;
import org.junit.jupiter.api.Test;

class LogbackCapturerFactoryTest {

  @Test
  void testCreate() {
    LogbackCapturerFactory factory = new LogbackCapturerFactory();
    QAPLogCapturer capturer = factory.create();

    assertNotNull(capturer);
    assertInstanceOf(LogbackCapturer.class, capturer);
  }

  @Test
  void testGetName() {
    LogbackCapturerFactory factory = new LogbackCapturerFactory();
    assertEquals("Logback", factory.getName());
  }

  @Test
  void testCreatedCapturerIsAvailable() {
    LogbackCapturerFactory factory = new LogbackCapturerFactory();
    QAPLogCapturer capturer = factory.create();

    assertTrue(capturer.isAvailable());
    assertEquals("Logback", capturer.getFrameworkName());
  }

  @Test
  void testMultipleCreations() {
    LogbackCapturerFactory factory = new LogbackCapturerFactory();

    QAPLogCapturer capturer1 = factory.create();
    QAPLogCapturer capturer2 = factory.create();

    assertNotNull(capturer1);
    assertNotNull(capturer2);
    assertNotSame(capturer1, capturer2, "Should create new instances");
  }
}
