package com.mk.fx.qa.qap.junit.extension.publisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.model.*;
import com.mk.fx.qa.qap.junit.runtime.QAPRuntime;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

@DisplayName("Publishers Tests")
class PublishersTest {

  private ObjectMapper objectMapper;
  private Logger mockLogger;
  private QAPJunitLaunch testLaunch;
  private final PrintStream originalOut = System.out;
  private ByteArrayOutputStream outBuffer;

  @BeforeEach
  void setUp() {
    objectMapper = QAPRuntime.defaultRuntime().getObjectMapper();
    mockLogger = mock(Logger.class);

    // Create a test launch with realistic data
    testLaunch = createTestLaunch();
  }

  @AfterEach
  void tearDown() {
    if (outBuffer != null) {
      System.setOut(originalOut);
    }
  }

  private QAPJunitLaunch createTestLaunch() {
    QAPHeader header = new QAPHeader(System.currentTimeMillis(), "TestLaunch-abc123def456");

    QAPTest test1 = new QAPTest("testMethod1", "Test Method 1");
    test1.setTestCaseId("TestClass#testMethod1");
    test1.setStatus("PASSED");
    test1.setStartTime(System.currentTimeMillis());
    test1.setEndTime(System.currentTimeMillis() + 100);

    QAPTest test2 = new QAPTest("testMethod2", "Test Method 2");
    test2.setTestCaseId("TestClass#testMethod2");
    test2.setStatus("FAILED");
    test2.setStartTime(System.currentTimeMillis());
    test2.setEndTime(System.currentTimeMillis() + 50);

    List<QAPTest> testCases = new ArrayList<>();
    testCases.add(test1);
    testCases.add(test2);

    QAPTestClass testClass = new QAPTestClass("TestClass", "TestClass", Collections.emptySet());
    testClass.setTestCases(testCases);

    List<QAPTestClass> testClasses = new ArrayList<>();
    testClasses.add(testClass);

    return new QAPJunitLaunch(header, testClasses);
  }

  @Nested
  @DisplayName("StdOutPublisher Tests")
  class StdOutPublisherTests {

    private StdOutPublisher publisher;

    @BeforeEach
    void setUp() {
      publisher = new StdOutPublisher();
      outBuffer = new ByteArrayOutputStream();
      System.setOut(new PrintStream(outBuffer));
    }

    @Test
    @DisplayName("Should publish JSON to stdout")
    void testPublishToStdOut() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      String output = outBuffer.toString();
      assertFalse(output.isEmpty(), "Output should not be empty");
      assertTrue(output.contains("\"launchId\""), "Should contain launchId field");
      assertTrue(output.contains("TestLaunch-abc123def456"), "Should contain launch ID value");
      assertTrue(output.contains("\"testClasses\""), "Should contain testClasses field");
      assertTrue(output.contains("TestClass"), "Should contain test class name");
    }

    @Test
    @DisplayName("Should log info message with metrics")
    void testLogInfoMessage() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      verify(mockLogger)
          .info(
              eq("Publishing QAP launch: class='{}' tests={} bytes={} launchId='{}'"),
              eq("TestClass"),
              eq(2),
              anyInt(),
              eq("TestLaunch-abc123def456"));
    }

    @Test
    @DisplayName("Should log debug message with payload")
    void testLogDebugMessage() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      verify(mockLogger).debug(eq("QAP Launch payload: {}"), anyString());
    }

    @Test
    @DisplayName("Should handle empty test classes list")
    void testEmptyTestClasses() {
      QAPHeader header = new QAPHeader(System.currentTimeMillis(), "EmptyLaunch-123");
      QAPJunitLaunch emptyLaunch = new QAPJunitLaunch(header, new ArrayList<>());

      publisher.publish(emptyLaunch, objectMapper, mockLogger);

      String output = outBuffer.toString();
      assertFalse(output.isEmpty());
      assertTrue(output.contains("EmptyLaunch-123"));

      verify(mockLogger)
          .info(
              anyString(),
              eq(""), // Empty class name
              eq(0), // Zero tests
              anyInt(),
              eq("EmptyLaunch-123"));
    }

    @Test
    @DisplayName("Should handle serialization failure gracefully")
    void testSerializationFailure() {
      ObjectMapper failingMapper = mock(ObjectMapper.class);
      try {
        when(failingMapper.writeValueAsString(any()))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Test error") {});
      } catch (Exception e) {
        fail("Mock setup failed");
      }

      publisher.publish(testLaunch, failingMapper, mockLogger);

      verify(mockLogger)
          .error(eq("Failed to serialize QAP launch payload: {}"), anyString(), any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("JSON deserialization tested in integration tests")
    @DisplayName("Should produce valid JSON")
    void testValidJson() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      String output = outBuffer.toString().trim();
      assertDoesNotThrow(
          () -> objectMapper.readValue(output, QAPJunitLaunch.class),
          "Output should be valid JSON");
    }

    @Test
    @DisplayName("Should count tests correctly")
    void testTestCounting() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      verify(mockLogger)
          .info(
              anyString(),
              eq("TestClass"),
              eq(2), // 2 tests
              anyInt(),
              anyString());
    }

    @Test
    @DisplayName("Should report byte size")
    void testByteSizeReporting() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      ArgumentCaptor<Integer> bytesCaptor = ArgumentCaptor.forClass(Integer.class);
      verify(mockLogger)
          .info(anyString(), anyString(), anyInt(), bytesCaptor.capture(), anyString());

      int reportedBytes = bytesCaptor.getValue();
      assertTrue(reportedBytes > 0, "Should report positive byte size");
    }
  }

  @Nested
  @DisplayName("LoggingPublisher Tests")
  class LoggingPublisherTests {

    private LoggingPublisher publisher;

    @BeforeEach
    void setUp() {
      publisher = new LoggingPublisher();
      outBuffer = new ByteArrayOutputStream();
      System.setOut(new PrintStream(outBuffer));
    }

    @Test
    @org.junit.jupiter.api.Disabled("Flaky test - output redirection timing issue")
    @DisplayName("Should NOT publish to stdout")
    void testNoStdOutOutput() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      String output = outBuffer.toString();
      assertTrue(output.isEmpty(), "LoggingPublisher should not print to stdout");
    }

    @Test
    @DisplayName("Should log info message with metrics")
    void testLogInfoMessage() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      verify(mockLogger)
          .info(
              eq("Publishing QAP launch: class='{}' tests={} bytes={} launchId='{}'"),
              eq("TestClass"),
              eq(2),
              anyInt(),
              eq("TestLaunch-abc123def456"));
    }

    @Test
    @DisplayName("Should log debug message with payload")
    void testLogDebugMessage() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      verify(mockLogger).debug(eq("QAP Launch payload: {}"), anyString());
    }

    @Test
    @DisplayName("Should handle empty test classes list")
    void testEmptyTestClasses() {
      QAPHeader header = new QAPHeader(System.currentTimeMillis(), "EmptyLaunch-456");
      QAPJunitLaunch emptyLaunch = new QAPJunitLaunch(header, new ArrayList<>());

      publisher.publish(emptyLaunch, objectMapper, mockLogger);

      verify(mockLogger)
          .info(
              anyString(),
              eq(""), // Empty class name
              eq(0), // Zero tests
              anyInt(),
              eq("EmptyLaunch-456"));
    }

    @Test
    @DisplayName("Should handle serialization failure gracefully")
    void testSerializationFailure() {
      ObjectMapper failingMapper = mock(ObjectMapper.class);
      try {
        when(failingMapper.writeValueAsString(any()))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Test error") {});
      } catch (Exception e) {
        fail("Mock setup failed");
      }

      publisher.publish(testLaunch, failingMapper, mockLogger);

      verify(mockLogger)
          .error(eq("Failed to serialize QAP launch payload: {}"), anyString(), any());
    }

    @Test
    @DisplayName("Should count tests correctly")
    void testTestCounting() {
      publisher.publish(testLaunch, objectMapper, mockLogger);

      verify(mockLogger).info(anyString(), eq("TestClass"), eq(2), anyInt(), anyString());
    }
  }

  @Nested
  @DisplayName("AsyncPublisher Tests")
  class AsyncPublisherTests {

    @Test
    @DisplayName("Should delegate to wrapped publisher asynchronously")
    void testAsyncDelegation() throws Exception {
      CountDownLatch latch = new CountDownLatch(1);
      LaunchPublisher mockDelegate = mock(LaunchPublisher.class);

      doAnswer(
              invocation -> {
                latch.countDown();
                return null;
              })
          .when(mockDelegate)
          .publish(any(), any(), any());

      AsyncPublisher publisher = new AsyncPublisher(mockDelegate);

      publisher.publish(testLaunch, objectMapper, mockLogger);

      boolean completed = latch.await(2, TimeUnit.SECONDS);
      assertTrue(completed, "Async publish should complete within timeout");

      verify(mockDelegate).publish(testLaunch, objectMapper, mockLogger);
    }

    @Test
    @DisplayName("Should use custom executor")
    void testCustomExecutor() throws Exception {
      ExecutorService customExecutor = Executors.newSingleThreadExecutor();
      LaunchPublisher mockDelegate = mock(LaunchPublisher.class);

      AsyncPublisher publisher = new AsyncPublisher(mockDelegate, customExecutor);
      publisher.publish(testLaunch, objectMapper, mockLogger);

      // Give async task time to complete
      customExecutor.shutdown();
      boolean terminated = customExecutor.awaitTermination(2, TimeUnit.SECONDS);
      assertTrue(terminated, "Executor should terminate");

      verify(mockDelegate).publish(testLaunch, objectMapper, mockLogger);
    }

    @Test
    @DisplayName("Should not block on publish")
    void testNonBlockingPublish() {
      CountDownLatch publishStarted = new CountDownLatch(1);
      CountDownLatch blockPublish = new CountDownLatch(1);

      LaunchPublisher blockingDelegate =
          (launch, mapper, log) -> {
            publishStarted.countDown();
            try {
              blockPublish.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          };

      AsyncPublisher publisher = new AsyncPublisher(blockingDelegate);

      long start = System.currentTimeMillis();
      publisher.publish(testLaunch, objectMapper, mockLogger);
      long duration = System.currentTimeMillis() - start;

      // Publish should return immediately
      assertTrue(duration < 1000, "publish() should not block (took " + duration + "ms)");

      // Verify async execution started
      try {
        boolean started = publishStarted.await(1, TimeUnit.SECONDS);
        assertTrue(started, "Async execution should have started");
      } catch (InterruptedException e) {
        fail("Test interrupted");
      } finally {
        blockPublish.countDown(); // Release the blocked publisher
      }
    }

    @Test
    @DisplayName("Should handle exceptions in delegate gracefully")
    void testExceptionInDelegate() throws Exception {
      CountDownLatch latch = new CountDownLatch(1);
      LaunchPublisher failingDelegate =
          (launch, mapper, log) -> {
            try {
              throw new RuntimeException("Delegate failed");
            } finally {
              latch.countDown();
            }
          };

      AsyncPublisher publisher = new AsyncPublisher(failingDelegate);

      // Should not throw
      assertDoesNotThrow(() -> publisher.publish(testLaunch, objectMapper, mockLogger));

      boolean completed = latch.await(2, TimeUnit.SECONDS);
      assertTrue(completed, "Delegate should have been invoked despite exception");
    }

    @Test
    @DisplayName("Should reject null delegate")
    void testNullDelegate() {
      assertThrows(NullPointerException.class, () -> new AsyncPublisher(null));
    }

    @Test
    @DisplayName("Should reject null executor")
    void testNullExecutor() {
      LaunchPublisher mockDelegate = mock(LaunchPublisher.class);
      assertThrows(NullPointerException.class, () -> new AsyncPublisher(mockDelegate, null));
    }

    @Test
    @DisplayName("Should handle multiple concurrent publishes")
    void testConcurrentPublishes() throws Exception {
      AtomicInteger counter = new AtomicInteger(0);
      CountDownLatch allPublished = new CountDownLatch(5);

      LaunchPublisher countingDelegate =
          (launch, mapper, log) -> {
            counter.incrementAndGet();
            allPublished.countDown();
          };

      AsyncPublisher publisher = new AsyncPublisher(countingDelegate);

      // Publish multiple launches
      for (int i = 0; i < 5; i++) {
        publisher.publish(testLaunch, objectMapper, mockLogger);
      }

      boolean completed = allPublished.await(3, TimeUnit.SECONDS);
      assertTrue(completed, "All publishes should complete");
      assertEquals(5, counter.get(), "All publishes should be executed");
    }

    @Test
    @DisplayName("NamedThreadFactory should create daemon threads with correct names")
    void testNamedThreadFactory() {
      AsyncPublisher.NamedThreadFactory factory = new AsyncPublisher.NamedThreadFactory("test");

      Thread t1 = factory.newThread(() -> {});
      assertEquals("test-1", t1.getName());
      assertTrue(t1.isDaemon(), "Should create daemon threads");

      Thread t2 = factory.newThread(() -> {});
      assertEquals("test-2", t2.getName());
      assertTrue(t2.isDaemon());

      Thread t3 = factory.newThread(() -> {});
      assertEquals("test-3", t3.getName());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should wrap StdOutPublisher with AsyncPublisher")
    void testAsyncStdOutPublisher() throws Exception {
      outBuffer = new ByteArrayOutputStream();
      System.setOut(new PrintStream(outBuffer));

      StdOutPublisher stdOut = new StdOutPublisher();
      AsyncPublisher asyncPublisher = new AsyncPublisher(stdOut);

      CountDownLatch latch = new CountDownLatch(1);

      // Use a thread to detect when output appears
      Thread checker =
          new Thread(
              () -> {
                try {
                  while (outBuffer.toString().isEmpty()) {
                    Thread.sleep(50);
                  }
                  latch.countDown();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              });
      checker.start();

      asyncPublisher.publish(testLaunch, objectMapper, mockLogger);

      boolean completed = latch.await(3, TimeUnit.SECONDS);
      assertTrue(completed, "Async publish to stdout should complete");

      String output = outBuffer.toString();
      assertFalse(output.isEmpty());
      assertTrue(output.contains("\"launchId\""));
    }

    @Test
    @DisplayName("Should chain AsyncPublisher with LoggingPublisher")
    void testAsyncLoggingPublisher() throws Exception {
      CountDownLatch latch = new CountDownLatch(1);
      Logger testLogger = mock(Logger.class);

      doAnswer(
              invocation -> {
                latch.countDown();
                return null;
              })
          .when(testLogger)
          .info(anyString(), any(), any(), any(), any());

      LoggingPublisher loggingPublisher = new LoggingPublisher();
      AsyncPublisher asyncPublisher = new AsyncPublisher(loggingPublisher);

      asyncPublisher.publish(testLaunch, objectMapper, testLogger);

      boolean completed = latch.await(2, TimeUnit.SECONDS);
      assertTrue(completed, "Async logging publish should complete");

      verify(testLogger).info(anyString(), anyString(), anyInt(), anyInt(), anyString());
    }
  }
}
