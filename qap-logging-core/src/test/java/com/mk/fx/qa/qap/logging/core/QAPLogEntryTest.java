package com.mk.fx.qa.qap.logging.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QAPLogEntryTest {

  @Test
  void testBuilder() {
    Instant now = Instant.now();
    QAPLogEntry entry =
        QAPLogEntry.builder()
            .timestamp(now)
            .level(QAPLogLevel.INFO)
            .loggerName("com.example.Test")
            .threadName("main")
            .message("Test message")
            .build();

    assertEquals(now, entry.getTimestamp());
    assertEquals(QAPLogLevel.INFO, entry.getLevel());
    assertEquals("com.example.Test", entry.getLoggerName());
    assertEquals("main", entry.getThreadName());
    assertEquals("Test message", entry.getMessage());
  }

  @Test
  void testBuilderWithMdcAndMarkers() {
    Instant now = Instant.now();
    Map<String, String> mdc = Map.of("requestId", "123", "userId", "456");
    Set<String> markers = Set.of("IMPORTANT", "AUDIT");

    QAPLogEntry entry =
        QAPLogEntry.builder()
            .timestamp(now)
            .level(QAPLogLevel.WARN)
            .loggerName("com.example.Service")
            .threadName("worker-1")
            .message("Warning message")
            .mdc(mdc)
            .markers(markers)
            .build();

    assertEquals(mdc, entry.getMdc());
    assertEquals(markers, entry.getMarkers());
  }

  @Test
  void testBuilderWithThrowable() {
    Instant now = Instant.now();
    String[] stackTrace = {"line1", "line2", "line3"};

    QAPLogEntry entry =
        QAPLogEntry.builder()
            .timestamp(now)
            .level(QAPLogLevel.ERROR)
            .loggerName("com.example.Error")
            .message("Error occurred")
            .throwableMessage("NullPointerException")
            .stackTrace(stackTrace)
            .build();

    assertEquals("NullPointerException", entry.getThrowableMessage());
    assertArrayEquals(stackTrace, entry.getStackTrace());
  }

  @Test
  void testImmutability() {
    Map<String, String> mdc = new java.util.HashMap<>();
    mdc.put("key", "value");

    QAPLogEntry entry =
        QAPLogEntry.builder()
            .timestamp(Instant.now())
            .level(QAPLogLevel.INFO)
            .loggerName("test")
            .message("test")
            .mdc(mdc)
            .build();

    // Try to modify original map - should not affect entry
    mdc.put("newKey", "newValue");

    assertEquals(1, entry.getMdc().size());
    assertFalse(entry.getMdc().containsKey("newKey"));
  }

  @Test
  void testRequiredFieldsNull() {
    assertThrows(
        NullPointerException.class,
        () ->
            QAPLogEntry.builder()
                .timestamp(null)
                .level(QAPLogLevel.INFO)
                .loggerName("test")
                .message("test")
                .build());

    assertThrows(
        NullPointerException.class,
        () ->
            QAPLogEntry.builder()
                .timestamp(Instant.now())
                .level(null)
                .loggerName("test")
                .message("test")
                .build());

    assertThrows(
        NullPointerException.class,
        () ->
            QAPLogEntry.builder()
                .timestamp(Instant.now())
                .level(QAPLogLevel.INFO)
                .loggerName(null)
                .message("test")
                .build());
  }

  @Test
  void testCompactString() {
    QAPLogEntry entry =
        QAPLogEntry.builder()
            .timestamp(Instant.now())
            .level(QAPLogLevel.INFO)
            .loggerName("com.example.Test")
            .message("Hello World")
            .build();

    String compact = entry.toCompactString();
    assertTrue(compact.contains("INFO"));
    assertTrue(compact.contains("com.example.Test"));
    assertTrue(compact.contains("Hello World"));
  }

  @Test
  void testEmptyMdcAndMarkers() {
    QAPLogEntry entry =
        QAPLogEntry.builder()
            .timestamp(Instant.now())
            .level(QAPLogLevel.INFO)
            .loggerName("test")
            .message("test")
            .build();

    assertNotNull(entry.getMdc());
    assertNotNull(entry.getMarkers());
    assertTrue(entry.getMdc().isEmpty());
    assertTrue(entry.getMarkers().isEmpty());
  }
}
