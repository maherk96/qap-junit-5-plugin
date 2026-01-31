package com.mk.fx.qa.qap.junit.extension;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.*;

import com.mk.fx.qa.qap.junit.core.QAPUtils;
import com.mk.fx.qa.qap.junit.model.QAPFailure;
import com.mk.fx.qa.qap.junit.model.QAPFixture;
import com.mk.fx.qa.qap.junit.model.QAPParameterization;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.model.QAPTestClass;
import com.mk.fx.qa.qap.junit.model.QAPTestParams;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import com.mk.fx.qa.qap.junit.util.ExceptionFormatter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Interceptor for test lifecycle methods (beforeAll, beforeEach, afterEach, afterAll). Captures
 * timing, status, and failures for each fixture execution.
 *
 * <p>Thread-safety: The failedInits map must be a ConcurrentHashMap to support parallel test
 * execution safely.
 */
public class QAPJunitMethodInterceptor implements IMethodInterceptor {

  /**
   * Pattern for detecting synthetic parameter names (arg0, arg1, etc.) generated when the
   * -parameters compiler flag is not used.
   */
  private static final Pattern SYNTHETIC_PARAM_PATTERN = Pattern.compile("arg\\d+");

  /**
   * Thread-safe map tracking failed initialization methods across test execution. Keys are
   * extension context unique IDs, values are the exceptions thrown. Must be periodically cleaned to
   * prevent memory leaks in long-running test suites.
   */
  private final Map<String, Throwable> failedInits;

  /**
   * Optional log capturer for capturing logs during fixture execution. Volatile ensures visibility
   * across threads. Null-safe with fallback to defaultConfig() if not set.
   */
  private volatile com.mk.fx.qa.qap.logging.core.QAPLogCapturer logCapturer;

  /**
   * Log capture configuration from qap.properties. Volatile ensures visibility across threads.
   * Null-safe with fallback to defaultConfig() if not set.
   */
  private volatile com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig logCaptureConfig;

  public QAPJunitMethodInterceptor(Map<String, Throwable> failedInits) {
    this.failedInits = failedInits;
  }

  /**
   * Sets the log capturer for capturing logs during fixtures. Called after initialization if
   * logging is available.
   */
  public void setLogCapturer(com.mk.fx.qa.qap.logging.core.QAPLogCapturer logCapturer) {
    this.logCapturer = logCapturer;
  }

  /**
   * Sets the log capture configuration from qap.properties. This ensures fixtures use the same log
   * level and filters as test execution.
   */
  public void setLogCaptureConfig(com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig config) {
    this.logCaptureConfig = config;
  }

  @Override
  public void interceptTestTemplateMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    var testParams = invocationContext.getArguments().toArray();
    var qapTest =
        StoreManager.getMethodStoreData(extensionContext, METHOD_DESCRIPTION_KEY, QAPTest.class);

    // Defensive null check - proceed if qapTest is null (shouldn't happen, but safe)
    if (qapTest == null) {
      invocation.proceed();
      return;
    }

    Method method = extensionContext.getRequiredTestMethod();
    Parameter[] parameters = method.getParameters();

    List<QAPTestParams> qapTestParams = new ArrayList<>();
    for (int i = 0; i < testParams.length; i++) {
      var arg = testParams[i];
      var argClassName = (arg != null) ? arg.getClass().getSimpleName() : "null";
      var argStringValue = (arg != null) ? arg.toString() : "null";

      // Extract parameter name if available (requires -parameters compiler flag)
      String paramName = null;
      if (i < parameters.length) {
        Parameter param = parameters[i];
        paramName = param.getName();
        // Use compiled pattern to check if name is synthetic
        if (paramName != null && SYNTHETIC_PARAM_PATTERN.matcher(paramName).matches()) {
          paramName = null;
        }
      }

      var params = new QAPTestParams(i, paramName, argClassName, argStringValue);
      qapTestParams.add(params);
    }
    qapTest.setParameters(qapTestParams);
    qapTest.setTestType("PARAMETERIZED");

    // Extract parameterization source/provider
    String provider = extractParameterizationProvider(method);

    // Compute a stable index per method invocation using the method-level store
    var methodStore = StoreManager.getMethodStore(extensionContext);
    Integer current = methodStore.get(QAPUtils.PARAM_INDEX_KEY, Integer.class);
    int index = (current == null) ? 0 : current + 1;
    methodStore.put(QAPUtils.PARAM_INDEX_KEY, index);

    // Set parameterization metadata
    if (provider != null) {
      qapTest.setParameterization(new QAPParameterization(provider, index));
    }

    // Build testCaseId as TopLevelClass#methodName[index]
    String fqcn = extensionContext.getRequiredTestClass().getName();
    String nestedPath = fqcn.substring(fqcn.lastIndexOf('.') + 1);
    String id =
        nestedPath + "#" + extensionContext.getRequiredTestMethod().getName() + "[" + index + "]";
    qapTest.setTestCaseId(id);

    // Capture logs for parameterized test execution
    String testId = extensionContext.getUniqueId();
    startFixtureLogCapture(testId);

    try {
      invocation.proceed();
    } finally {
      java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> testLogs =
          stopFixtureLogCapture(testId);
      attachLogsToTest(extensionContext, testLogs);
    }
  }

  @Override
  public void interceptTestMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    // Capture logs for the actual test method execution
    String testId = extensionContext.getUniqueId();
    startFixtureLogCapture(testId);

    try {
      invocation.proceed();
    } finally {
      java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> testLogs =
          stopFixtureLogCapture(testId);
      attachLogsToTest(extensionContext, testLogs);
    }
  }

  /**
   * Attaches captured logs to the test's lifecycle execution. Ensures the test execution object
   * exists before attaching logs. Null-safe - handles missing test or lifecycle gracefully.
   *
   * @param context the extension context for the test
   * @param logs the captured log entries (may be null)
   */
  private void attachLogsToTest(
      ExtensionContext context, java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs) {
    QAPTest qapTest =
        StoreManager.getMethodStoreData(context, METHOD_DESCRIPTION_KEY, QAPTest.class);

    if (qapTest == null || qapTest.getLifecycle() == null) {
      return; // Nothing to attach to
    }

    // Ensure TestExecution object exists
    if (qapTest.getLifecycle().getTest() == null) {
      qapTest
          .getLifecycle()
          .setTest(new com.mk.fx.qa.qap.junit.model.QAPTestLifecycle.TestExecution());
    }

    // Attach test-only logs (not including beforeEach/afterEach)
    qapTest.getLifecycle().getTest().setLogEntries(logs);
  }

  /**
   * Extracts the parameterization provider name from method annotations. Checks for common JUnit 5
   * parameterization annotations.
   */
  private String extractParameterizationProvider(Method method) {
    if (method.getAnnotation(CsvSource.class) != null) {
      return "CsvSource";
    }
    if (method.getAnnotation(MethodSource.class) != null) {
      return "MethodSource";
    }
    if (method.getAnnotation(ValueSource.class) != null) {
      return "ValueSource";
    }
    if (method.getAnnotation(ArgumentsSource.class) != null) {
      return "ArgumentsSource";
    }
    if (method.getAnnotation(EnumSource.class) != null) {
      return "EnumSource";
    }
    if (method.getAnnotation(NullSource.class) != null) {
      return "NullSource";
    }
    if (method.getAnnotation(EmptySource.class) != null) {
      return "EmptySource";
    }
    if (method.getAnnotation(NullAndEmptySource.class) != null) {
      return "NullAndEmptySource";
    }
    // Check for composite sources (multiple annotations)
    if (method.getAnnotations().length > 0) {
      // Look for any annotation that might be a parameter source
      for (var annotation : method.getAnnotations()) {
        String annotationName = annotation.annotationType().getSimpleName();
        if (annotationName.contains("Source") || annotationName.contains("Provider")) {
          return annotationName;
        }
      }
    }
    return null;
  }

  @Override
  public void interceptBeforeAllMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();
    var classStore = StoreManager.getClassStore(extensionContext);
    classStore.put(FIXTURE_START_TIME_KEY, startTime);
    classStore.put(FIXTURE_START_TIME_NANOS_KEY, startTimeNanos);

    // Get the fixture method for metadata
    Method fixtureMethod = (Method) invocationContext.getExecutable();

    // Start log capture for this fixture
    String fixtureId = extensionContext.getUniqueId() + ":beforeAll:" + fixtureMethod.getName();
    startFixtureLogCapture(fixtureId);

    Throwable failure = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      failure = t;
      failedInits.put(extensionContext.getUniqueId(), t);
      throw t;
    } finally {
      long endTime = System.currentTimeMillis();
      long endTimeNanos = System.nanoTime();
      long durationNanos = endTimeNanos - startTimeNanos;

      // Stop log capture and get logs
      java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs =
          stopFixtureLogCapture(fixtureId);

      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;

      // Create class-level fixture with full metadata
      QAPFixture fixture =
          new QAPFixture(
              "BEFORE_ALL",
              fixtureMethod.getName(),
              fixtureMethod.getDeclaringClass().getName(),
              failure != null ? "FAILED" : "PASSED",
              durationNanos,
              qapFailure,
              logs);
      addFixtureToClass(extensionContext, fixture, fixtureMethod);
    }
  }

  @Override
  public void interceptBeforeEachMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();
    var methodStore = StoreManager.getMethodStore(extensionContext);
    methodStore.put(FIXTURE_START_TIME_KEY, startTime);
    methodStore.put(FIXTURE_START_TIME_NANOS_KEY, startTimeNanos);

    // Start log capture for this fixture
    String fixtureId = extensionContext.getUniqueId() + ":beforeEach:" + System.nanoTime();
    startFixtureLogCapture(fixtureId);

    Throwable failure = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      failure = t;
      // Link BeforeEach failure to the test case
      QAPTest qapTest =
          StoreManager.getMethodStoreData(extensionContext, METHOD_DESCRIPTION_KEY, QAPTest.class);
      if (qapTest != null) {
        qapTest.setFailure(ExceptionFormatter.toFailure(t));
        qapTest.setStatus("FAILED");
      }
      throw t;
    } finally {
      long endTime = System.currentTimeMillis();
      long endTimeNanos = System.nanoTime();
      long durationNanos = endTimeNanos - startTimeNanos;

      // Stop log capture and get logs
      java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs =
          stopFixtureLogCapture(fixtureId);

      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;

      // Get the fixture method from the invocation context
      Method fixtureMethod = (Method) invocationContext.getExecutable();

      // Add fixture to the current test case's lifecycle
      addFixtureToTest(
          extensionContext,
          "BEFORE_EACH",
          fixtureMethod,
          failure != null ? "FAILED" : "PASSED",
          durationNanos,
          qapFailure,
          logs);
    }
  }

  @Override
  public void interceptAfterEachMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();

    // Start log capture for this fixture
    String fixtureId = extensionContext.getUniqueId() + ":afterEach:" + System.nanoTime();
    startFixtureLogCapture(fixtureId);

    Throwable failure = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      failure = t;
      throw t;
    } finally {
      long endTime = System.currentTimeMillis();
      long endTimeNanos = System.nanoTime();
      long durationNanos = endTimeNanos - startTimeNanos;

      // Stop log capture and get logs
      java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs =
          stopFixtureLogCapture(fixtureId);

      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;

      // Get the fixture method from the invocation context
      Method fixtureMethod = (Method) invocationContext.getExecutable();

      // Add fixture to the current test case's lifecycle
      addFixtureToTest(
          extensionContext,
          "AFTER_EACH",
          fixtureMethod,
          failure != null ? "FAILED" : "PASSED",
          durationNanos,
          qapFailure,
          logs);
    }
  }

  @Override
  public void interceptAfterAllMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();
    var classStore = StoreManager.getClassStore(extensionContext);
    classStore.put(FIXTURE_START_TIME_KEY, startTime);
    classStore.put(FIXTURE_START_TIME_NANOS_KEY, startTimeNanos);

    // Get the fixture method for metadata
    Method fixtureMethod = (Method) invocationContext.getExecutable();

    // Start log capture for this fixture
    String fixtureId = extensionContext.getUniqueId() + ":afterAll:" + fixtureMethod.getName();
    startFixtureLogCapture(fixtureId);

    Throwable failure = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      failure = t;
      throw t;
    } finally {
      long endTime = System.currentTimeMillis();
      long endTimeNanos = System.nanoTime();
      long durationNanos = endTimeNanos - startTimeNanos;

      // Stop log capture and get logs
      java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs =
          stopFixtureLogCapture(fixtureId);

      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;

      // Create class-level fixture with full metadata
      QAPFixture fixture =
          new QAPFixture(
              "AFTER_ALL",
              fixtureMethod.getName(),
              fixtureMethod.getDeclaringClass().getName(),
              failure != null ? "FAILED" : "PASSED",
              durationNanos,
              qapFailure,
              logs);
      addFixtureToClass(extensionContext, fixture, fixtureMethod);

      // Clean up failed init tracking for this context to prevent memory leak
      failedInits.remove(extensionContext.getUniqueId());
    }
  }

  /**
   * Adds a fixture to the current test case's lifecycle. This links fixtures directly to the test
   * that was running when they executed.
   *
   * <p>NOTE: This is only for @BeforeEach and @AfterEach fixtures. @BeforeAll and @AfterAll are
   * class-level and handled by addFixtureToClass.
   */
  private void addFixtureToTest(
      ExtensionContext context,
      String phase,
      Method fixtureMethod,
      String status,
      long durationNanos,
      QAPFailure error,
      java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs) {
    // Get the current test case from method store
    QAPTest qapTest =
        StoreManager.getMethodStoreData(context, METHOD_DESCRIPTION_KEY, QAPTest.class);
    if (qapTest == null) {
      // No test case available - shouldn't happen for beforeEach/afterEach
      return;
    }

    // Ensure lifecycle is initialized
    if (qapTest.getLifecycle() == null) {
      qapTest.setLifecycle(new com.mk.fx.qa.qap.junit.model.QAPTestLifecycle());
    }

    // Create test fixture with method and class information
    String methodName = fixtureMethod.getName();
    String className = fixtureMethod.getDeclaringClass().getName(); // ✅ Use FQN for consistency

    // Determine order based on current list size
    int order;
    java.util.List<com.mk.fx.qa.qap.junit.model.QAPTestFixture> targetList;
    if ("BEFORE_EACH".equals(phase)) {
      targetList = qapTest.getLifecycle().getBeforeEach();
      order = targetList.size() + 1;
    } else if ("AFTER_EACH".equals(phase)) {
      targetList = qapTest.getLifecycle().getAfterEach();
      order = targetList.size() + 1;
    } else {
      // Invalid phase for test-level fixtures
      return;
    }

    com.mk.fx.qa.qap.junit.model.QAPTestFixture testFixture =
        new com.mk.fx.qa.qap.junit.model.QAPTestFixture(
            methodName, className, order, status, durationNanos, error, logs);
    targetList.add(testFixture);
  }

  /**
   * Convenience overload for fixtures without log capture. Delegates to full method with null logs.
   */
  private void addFixtureToTest(
      ExtensionContext context,
      String phase,
      Method fixtureMethod,
      String status,
      long durationNanos,
      QAPFailure error) {
    addFixtureToTest(context, phase, fixtureMethod, status, durationNanos, error, null);
  }

  /**
   * Adds a class-level fixture (beforeAll/afterAll) to the test class metadata. Uses
   * computeIfAbsent to safely handle concurrent access to the class nodes map.
   */
  private void addFixtureToClass(
      ExtensionContext context, QAPFixture fixture, Method fixtureMethod) {
    var classStore = StoreManager.getClassStore(context);

    // Use getOrComputeIfAbsent to safely initialize the map (thread-safe)
    @SuppressWarnings("unchecked")
    Map<String, QAPTestClass> nodes =
        (Map<String, QAPTestClass>)
            classStore.getOrComputeIfAbsent(
                CLASS_NODES_KEY,
                k -> new java.util.concurrent.ConcurrentHashMap<String, QAPTestClass>());

    // Record fixture in the test class where the test is currently running
    Class<?> testClass = context.getRequiredTestClass();
    String testClassKey = testClass.getName();

    QAPTestClass testClassNode =
        nodes.computeIfAbsent(
            testClassKey,
            k -> {
              QAPTestClass newNode =
                  new QAPTestClass(
                      testClass.getSimpleName(),
                      testClass.getSimpleName(),
                      java.util.Collections.emptySet());
              newNode.setClassFqn(testClassKey);
              newNode.setClassSimpleName(testClass.getSimpleName());
              newNode.setClassKey(testClassKey);
              newNode.setFixtures(new ArrayList<>());
              return newNode;
            });

    // Ensure fixtures list is initialized
    if (testClassNode.getFixtures() == null) {
      testClassNode.setFixtures(new ArrayList<>());
    }

    // Record fixture - map updates are reflected in store via reference
    testClassNode.getFixtures().add(fixture);
  }

  // ---- Log Capture Helpers -----------------------------------------------

  /**
   * Starts log capture for a fixture execution. Creates a unique capture ID to isolate fixture
   * logs. Uses the same configuration as test execution (from qap.properties).
   */
  private void startFixtureLogCapture(String fixtureId) {
    if (logCapturer == null) {
      return; // No capturer available
    }

    try {
      // Use configured log level from qap.properties, or default if not set
      com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig config =
          (logCaptureConfig != null)
              ? logCaptureConfig
              : com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig.defaultConfig();
      logCapturer.startCapture(fixtureId, config);
    } catch (Exception e) {
      // Never fail tests due to logging issues - just skip capture
    }
  }

  /**
   * Stops log capture for a fixture and returns captured logs. Returns null if capture was not
   * started or failed.
   */
  private java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> stopFixtureLogCapture(
      String fixtureId) {
    if (logCapturer == null) {
      return null;
    }

    try {
      return logCapturer.stopCapture(fixtureId);
    } catch (Exception e) {
      // Never fail tests due to logging issues
      return null;
    }
  }
}
