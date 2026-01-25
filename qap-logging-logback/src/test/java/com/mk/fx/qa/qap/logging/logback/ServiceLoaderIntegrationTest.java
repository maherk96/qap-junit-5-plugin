package com.mk.fx.qa.qap.logging.logback;

import static org.junit.jupiter.api.Assertions.*;

import com.mk.fx.qa.qap.logging.core.QAPLogCapturer;
import com.mk.fx.qa.qap.logging.core.QAPLogCapturerFactory;
import com.mk.fx.qa.qap.logging.core.QAPLogCapturerRegistry;
import java.util.Optional;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class ServiceLoaderIntegrationTest {

  @Test
  void testServiceLoaderFindsLogbackFactory() {
    ServiceLoader<QAPLogCapturerFactory> loader = ServiceLoader.load(QAPLogCapturerFactory.class);

    boolean found = false;
    for (QAPLogCapturerFactory factory : loader) {
      if (factory instanceof LogbackCapturerFactory) {
        found = true;
        assertEquals("Logback", factory.getName());
      }
    }

    assertTrue(found, "ServiceLoader should discover LogbackCapturerFactory");
  }

  @Test
  void testRegistryDiscoversLogback() {
    QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();
    registry.discover();

    assertTrue(registry.getAvailableCount() > 0, "Registry should discover at least one capturer");

    Optional<QAPLogCapturer> logbackCapturer = registry.getCapturerByName("Logback");
    assertTrue(logbackCapturer.isPresent(), "Registry should have Logback capturer");
    assertEquals("Logback", logbackCapturer.get().getFrameworkName());
  }

  @Test
  void testRegistryReturnsAvailableLogback() {
    QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();
    registry.discover();

    Optional<QAPLogCapturer> capturer = registry.getAvailableCapturer();
    assertTrue(capturer.isPresent(), "Registry should return an available capturer");

    // Should be Logback or Log4j2 depending on what's on classpath
    String framework = capturer.get().getFrameworkName();
    assertTrue(
        "Logback".equals(framework) || "Log4j2".equals(framework),
        "Should be either Logback or Log4j2");
  }

  @Test
  void testLogbackCapturerIsAvailable() {
    QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();
    registry.discover();

    Optional<QAPLogCapturer> capturer = registry.getCapturerByName("Logback");
    assertTrue(capturer.isPresent());
    assertTrue(capturer.get().isAvailable(), "Logback capturer should be available");
  }

  @Test
  void testLogbackCapturerPriority() {
    QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();
    registry.discover();

    Optional<QAPLogCapturer> capturer = registry.getCapturerByName("Logback");
    assertTrue(capturer.isPresent());
    assertEquals(0, capturer.get().getPriority(), "Logback should have default priority 0");
  }

  @Test
  void testMultipleDiscoveryCalls() {
    QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();

    registry.discover();
    int count1 = registry.getAvailableCount();

    registry.discover(); // Should be idempotent
    int count2 = registry.getAvailableCount();

    assertEquals(count1, count2, "Multiple discover() calls should be idempotent");
  }

  @Test
  void testGetAllCapturers() {
    QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();
    registry.discover();

    var allCapturers = registry.getAllAvailableCapturers();
    assertFalse(allCapturers.isEmpty(), "Should have at least one capturer");

    boolean hasLogback =
        allCapturers.stream().anyMatch(c -> "Logback".equals(c.getFrameworkName()));
    assertTrue(hasLogback, "Should include Logback capturer");
  }

  @Test
  void testGetCapturerByNonExistentName() {
    QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();
    registry.discover();

    Optional<QAPLogCapturer> capturer = registry.getCapturerByName("NonExistent");
    assertFalse(capturer.isPresent(), "Should return empty for non-existent framework");
  }

  @Test
  void testLogbackCapturerCanStartAndStop() {
    QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();
    registry.discover();

    Optional<QAPLogCapturer> capturerOpt = registry.getCapturerByName("Logback");
    assertTrue(capturerOpt.isPresent());

    QAPLogCapturer capturer = capturerOpt.get();

    // Should not throw
    assertDoesNotThrow(
        () -> {
          capturer.startCapture(
              "test-id", com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig.defaultConfig());
          capturer.stopCapture("test-id");
          capturer.shutdown();
        });
  }

  @Test
  void testLogbackFactoryMetaInfServicesFile() {
    // This test verifies the META-INF/services file exists and is correct
    ServiceLoader<QAPLogCapturerFactory> loader = ServiceLoader.load(QAPLogCapturerFactory.class);

    long logbackCount =
        loader.stream()
            .map(ServiceLoader.Provider::get)
            .filter(f -> "Logback".equals(f.getName()))
            .count();

    assertEquals(1, logbackCount, "Should have exactly one Logback factory registered");
  }
}
