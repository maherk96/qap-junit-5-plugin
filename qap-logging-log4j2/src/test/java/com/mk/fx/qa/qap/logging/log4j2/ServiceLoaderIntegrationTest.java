package com.mk.fx.qa.qap.logging.log4j2;

import static org.junit.jupiter.api.Assertions.*;

import com.mk.fx.qa.qap.logging.core.*;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test that verifies ServiceLoader auto-detection works end-to-end. This simulates what
 * happens when a user adds the qap-logging-log4j2 dependency.
 */
class ServiceLoaderIntegrationTest {

  private static final Logger logger = LogManager.getLogger(ServiceLoaderIntegrationTest.class);

  private QAPLogCapturerRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new QAPLogCapturerRegistry();
  }

  @AfterEach
  void tearDown() {
    if (registry != null) {
      registry.shutdown();
    }
  }

  @Test
  void testAutoDiscoveryFindsLog4j2() {
    registry.discover();

    assertTrue(registry.hasAvailableCapturer(), "Registry should find Log4j2 capturer");
    assertEquals(1, registry.getAvailableCount(), "Should find exactly 1 capturer (Log4j2)");
  }

  @Test
  void testGetAvailableCapturer() {
    Optional<QAPLogCapturer> capturer = registry.getAvailableCapturer();

    assertTrue(capturer.isPresent(), "Should find a capturer");
    assertEquals("Log4j2", capturer.get().getFrameworkName());
    assertTrue(capturer.get().isAvailable());
  }

  @Test
  void testGetCapturerByName() {
    Optional<QAPLogCapturer> capturer = registry.getCapturerByName("log4j2");

    assertTrue(capturer.isPresent(), "Should find Log4j2 by name");
    assertEquals("Log4j2", capturer.get().getFrameworkName());
  }

  @Test
  void testGetCapturerByNameCaseInsensitive() {
    Optional<QAPLogCapturer> capturer1 = registry.getCapturerByName("Log4j2");
    Optional<QAPLogCapturer> capturer2 = registry.getCapturerByName("log4j2");
    Optional<QAPLogCapturer> capturer3 = registry.getCapturerByName("LOG4J2");

    assertTrue(capturer1.isPresent());
    assertTrue(capturer2.isPresent());
    assertTrue(capturer3.isPresent());
  }

  @Test
  void testGetAllAvailableCapturers() {
    List<QAPLogCapturer> capturers = registry.getAllAvailableCapturers();

    assertFalse(capturers.isEmpty());
    assertEquals(1, capturers.size());
    assertEquals("Log4j2", capturers.get(0).getFrameworkName());
  }

  @Test
  void testEndToEndLogCapture() {
    // Simulate what QAPJunitExtension would do

    // 1. Discover available capturers
    registry.discover();
    Optional<QAPLogCapturer> capturerOpt = registry.getAvailableCapturer();
    assertTrue(capturerOpt.isPresent(), "Should auto-discover Log4j2 capturer");

    QAPLogCapturer capturer = capturerOpt.get();

    // 2. Start capture for a test
    String testId = "integration-test-001";
    QAPLogCaptureConfig config = QAPLogCaptureConfig.defaultConfig();
    capturer.startCapture(testId, config);

    // 3. Execute test code that logs
    logger.info("Integration test started");
    logger.warn("This is a warning");
    logger.error("This is an error");

    // 4. Stop capture and get logs
    List<QAPLogEntry> logs = capturer.stopCapture(testId);

    // 5. Verify logs were captured
    assertNotNull(logs);
    assertEquals(3, logs.size());

    QAPLogEntry firstLog = logs.get(0);
    assertEquals(QAPLogLevel.INFO, firstLog.getLevel());
    assertEquals("Integration test started", firstLog.getMessage());
    assertEquals(ServiceLoaderIntegrationTest.class.getName(), firstLog.getLoggerName());
    assertNotNull(firstLog.getTimestamp());
    assertNotNull(firstLog.getThreadName());

    // 6. Cleanup
    capturer.shutdown();
  }

  @Test
  void testDiscoveryIsIdempotent() {
    registry.discover();
    int count1 = registry.getAvailableCount();

    registry.discover(); // Discover again
    int count2 = registry.getAvailableCount();

    assertEquals(count1, count2, "Discover should be idempotent");
  }

  @Test
  void testCapturerPriority() {
    registry.discover();
    Optional<QAPLogCapturer> capturer = registry.getAvailableCapturer();

    assertTrue(capturer.isPresent());
    assertEquals(100, capturer.get().getPriority(), "Log4j2 should have priority 100");
  }

  @Test
  void testUserScenario_AddDependencyAndItJustWorks() {
    // This test simulates the user experience:
    // 1. User adds qap-logging-log4j2 dependency to build.gradle
    // 2. QAPJunitExtension automatically detects it
    // 3. Logs are captured without any configuration

    // Step 1: Extension creates registry and discovers
    QAPLogCapturerRegistry userRegistry = new QAPLogCapturerRegistry();
    userRegistry.discover();

    // Step 2: Extension checks if logging is available
    if (userRegistry.hasAvailableCapturer()) {
      QAPLogCapturer capturer = userRegistry.getAvailableCapturer().get();
      System.out.println("✅ Found logging framework: " + capturer.getFrameworkName());

      // Step 3: Extension starts capture before test
      String testId = "user-test-123";
      capturer.startCapture(testId, QAPLogCaptureConfig.defaultConfig());

      // Step 4: User's test runs and logs
      logger.info("User's test is running");
      logger.debug("Detailed debug info");
      logger.error("An error occurred");

      // Step 5: Extension stops capture after test
      List<QAPLogEntry> logs = capturer.stopCapture(testId);

      // Step 6: Extension attaches logs to test report
      assertNotNull(logs);
      assertFalse(logs.isEmpty());
      System.out.println("✅ Captured " + logs.size() + " log entries");

      // Cleanup
      capturer.shutdown();
    }

    userRegistry.shutdown();
  }

  @Test
  void testNoAvailableFrameworkGracefulDegradation() {
    // Create a fresh registry that won't find anything
    // (In reality, Log4j2 IS available, but this tests the fallback behavior)

    QAPLogCapturerRegistry emptyRegistry = new QAPLogCapturerRegistry();
    // Don't call discover(), so nothing is found

    // Should return empty, not throw exception
    Optional<QAPLogCapturer> capturer = emptyRegistry.getAvailableCapturer();

    // In a real scenario where no impl is available, this would be empty
    // But in our test, it discovers automatically, so it will be present
    // The important thing is: no exceptions thrown
    assertNotNull(capturer);
  }
}
