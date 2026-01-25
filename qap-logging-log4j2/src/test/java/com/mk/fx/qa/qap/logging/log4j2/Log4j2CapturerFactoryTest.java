package com.mk.fx.qa.qap.logging.log4j2;

import static org.junit.jupiter.api.Assertions.*;

import com.mk.fx.qa.qap.logging.core.QAPLogCapturer;
import org.junit.jupiter.api.Test;

class Log4j2CapturerFactoryTest {

  @Test
  void testFactoryCreate() {
    Log4j2CapturerFactory factory = new Log4j2CapturerFactory();

    QAPLogCapturer capturer = factory.create();

    assertNotNull(capturer);
    assertTrue(capturer instanceof Log4j2Capturer);
  }

  @Test
  void testFactoryName() {
    Log4j2CapturerFactory factory = new Log4j2CapturerFactory();

    assertEquals("Log4j2", factory.getName());
  }

  @Test
  void testCreatedCapturerIsAvailable() {
    Log4j2CapturerFactory factory = new Log4j2CapturerFactory();
    QAPLogCapturer capturer = factory.create();

    assertTrue(capturer.isAvailable(), "Created capturer should be available in test environment");
  }

  @Test
  void testCreatedCapturerHasCorrectFrameworkName() {
    Log4j2CapturerFactory factory = new Log4j2CapturerFactory();
    QAPLogCapturer capturer = factory.create();

    assertEquals("Log4j2", capturer.getFrameworkName());
  }
}
