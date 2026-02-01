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
 * Intercepts JUnit 5 lifecycle methods to capture detailed fixture execution data.
 *
 * <p>This interceptor captures:
 *
 * <ul>
 *   <li>Timing (start, end, duration in nanoseconds) for all fixtures and test executions
 *   <li>Success/failure status for each lifecycle phase
 *   <li>Exception details for failed fixtures and tests
 *   <li>Log output during fixture and test execution (if log capture enabled)
 *   <li>Parameterization metadata for {@code @ParameterizedTest} methods
 * </ul>
 *
 * <p><strong>Fixture Categorization:</strong>
 *
 * <ul>
 *   <li><strong>Class-level fixtures:</strong> {@code @BeforeAll} and {@code @AfterAll} are
 *       attached to {@link QAPTestClass} via {@link #addFixtureToClass}
 *   <li><strong>Test-level fixtures:</strong> {@code @BeforeEach} and {@code @AfterEach} are
 *       attached to {@link QAPTest} lifecycle via {@link #addFixtureToTest}
 *   <li><strong>Test executions:</strong> Regular {@code @Test} and {@code @ParameterizedTest}
 *       methods have logs attached via {@link #attachLogsToTest}
 * </ul>
 *
 * <p><strong>Parameterized Test Handling:</strong>
 *
 * <p>For {@code @ParameterizedTest} methods, this interceptor:
 *
 * <ul>
 *   <li>Extracts parameter values and types from test invocation
 *   <li>Captures parameter names if available (requires {@code -parameters} compiler flag)
 *   <li>Detects the parameter source annotation (CsvSource, MethodSource, ValueSource, etc.)
 *   <li>Assigns stable invocation indices per test method
 *   <li>Generates unique test case IDs in format: {@code ClassName#methodName[index]}
 * </ul>
 *
 * <p><strong>Thread Safety:</strong>
 *
 * <ul>
 *   <li>The {@code failedInits} map must be a {@link java.util.concurrent.ConcurrentHashMap} to
 *       support parallel test execution
 *   <li>Cleanup happens in {@link #interceptAfterAllMethod} to prevent memory leaks in long-running
 *       test suites
 *   <li>Log capturer and config use {@code volatile} for visibility across threads, with null-safe
 *       fallbacks to default configuration
 *   <li>Class nodes map uses {@code getOrComputeIfAbsent} for thread-safe initialization
 * </ul>
 *
 * <p><strong>Log Capture:</strong>
 *
 * <p>Log capture is optional and configured via {@link #setLogCapturer} and {@link
 * #setLogCaptureConfig}. Configuration comes from {@code qap.properties}. If not configured,
 * fixtures and tests are tracked without logs. Logging failures never cause test failures - errors
 * are silently ignored to ensure test execution proceeds.
 *
 * <p><strong>Memory Management:</strong>
 *
 * <p>The {@code failedInits} map tracks {@code @BeforeAll} failures to coordinate error handling
 * across lifecycle phases. Cleanup occurs in {@code interceptAfterAllMethod}. In rare cases where
 * {@code @AfterAll} doesn't execute (test suite aborted), entries may persist. This is a known
 * limitation for edge cases and acceptable given the rarity.
 *
 * @see QAPJunitExtension
 * @see QAPTestClass
 * @see QAPTest
 * @see IMethodInterceptor
 */
public class QAPJunitMethodInterceptor implements IMethodInterceptor {

  /**
   * Pattern for detecting synthetic parameter names (arg0, arg1, etc.) generated when the {@code
   * -parameters} compiler flag is not used.
   */
  private static final Pattern SYNTHETIC_PARAM_PATTERN = Pattern.compile("arg\\d+");

  /**
   * Thread-safe map tracking failed {@code @BeforeAll} methods across test execution.
   *
   * <p>Keys are extension context unique IDs, values are the exceptions thrown. This map
   * coordinates error handling between lifecycle phases. Cleaned in {@link
   * #interceptAfterAllMethod} to prevent memory leaks.
   */
  private final Map<String, Throwable> failedInits;

  /**
   * Optional log capturer for capturing logs during fixture and test execution.
   *
   * <p>Volatile ensures visibility across threads. Null-safe - falls back to {@code
   * defaultConfig()} if not set. Configured via {@link #setLogCapturer} after initialization.
   */
  private volatile com.mk.fx.qa.qap.logging.core.QAPLogCapturer logCapturer;

  /**
   * Log capture configuration from {@code qap.properties}.
   *
   * <p>Volatile ensures visibility across threads. Null-safe - falls back to {@code
   * defaultConfig()} if not set. Configured via {@link #setLogCaptureConfig} to ensure fixtures use
   * the same log level and filters as test execution.
   */
  private volatile com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig logCaptureConfig;

  /**
   * Creates a new method interceptor with the specified failed initialization tracker.
   *
   * @param failedInits thread-safe map for tracking {@code @BeforeAll} failures (must be
   *     ConcurrentHashMap)
   */
  public QAPJunitMethodInterceptor(Map<String, Throwable> failedInits) {
    this.failedInits = failedInits;
  }

  /**
   * Sets the log capturer for capturing logs during fixtures and test execution.
   *
   * <p>Called by {@link QAPJunitExtension} after log capture initialization if a logging framework
   * is available. Optional - if not set, fixtures and tests are tracked without logs.
   *
   * @param logCapturer the log capturer instance, or null to disable log capture
   */
  public void setLogCapturer(com.mk.fx.qa.qap.logging.core.QAPLogCapturer logCapturer) {
    this.logCapturer = logCapturer;
  }

  /**
   * Sets the log capture configuration from {@code qap.properties}.
   *
   * <p>Ensures fixtures use the same log level and filters as test execution. If not set, falls
   * back to {@code defaultConfig()}.
   *
   * @param config the log capture configuration
   */
  public void setLogCaptureConfig(com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig config) {
    this.logCaptureConfig = config;
  }

  /**
   * Intercepts {@code @ParameterizedTest} template method execution to capture parameterization
   * metadata, timing, and logs.
   *
   * <p>Called by JUnit for each invocation of a parameterized test. This method:
   *
   * <ul>
   *   <li>Extracts parameter values, types, and names (if available)
   *   <li>Detects the parameter source annotation (CsvSource, MethodSource, etc.)
   *   <li>Assigns a stable invocation index for this test method
   *   <li>Generates a unique test case ID: {@code ClassName#methodName[index]}
   *   <li>Captures logs during test execution
   *   <li>Attaches logs to the test lifecycle
   * </ul>
   *
   * <p><strong>Parameter Name Extraction:</strong>
   *
   * <p>Parameter names are only available if the code is compiled with {@code -parameters} flag.
   * Otherwise, synthetic names (arg0, arg1, etc.) are detected and excluded.
   *
   * @param invocation the parameterized test invocation to execute
   * @param invocationContext reflective context containing method and parameter information
   * @param extensionContext the extension context for this test
   * @throws Throwable if the intercepted test method throws
   */
  @Override
  public void interceptTestTemplateMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    QAPTest qapTest =
        StoreManager.getMethodStoreData(extensionContext, METHOD_DESCRIPTION_KEY, QAPTest.class);

    if (qapTest == null) {
      invocation.proceed();
      return;
    }

    Method method = extensionContext.getRequiredTestMethod();
    Parameter[] parameters = method.getParameters();
    Object[] testParams = invocationContext.getArguments().toArray();

    List<QAPTestParams> qapTestParams = new ArrayList<>();
    for (int i = 0; i < testParams.length; i++) {
      Object arg = testParams[i];
      String argClassName = (arg != null) ? arg.getClass().getSimpleName() : "null";
      String argStringValue = (arg != null) ? arg.toString() : "null";

      String paramName = null;
      if (i < parameters.length) {
        Parameter param = parameters[i];
        paramName = param.getName();
        if (paramName != null && SYNTHETIC_PARAM_PATTERN.matcher(paramName).matches()) {
          paramName = null;
        }
      }

      QAPTestParams params = new QAPTestParams(i, paramName, argClassName, argStringValue);
      qapTestParams.add(params);
    }

    qapTest.setParameters(qapTestParams);
    qapTest.setTestType("PARAMETERIZED");

    String provider = extractParameterizationProvider(method);

    ExtensionContext.Store methodStore = StoreManager.getMethodStore(extensionContext);
    Integer current = methodStore.get(QAPUtils.PARAM_INDEX_KEY, Integer.class);
    int index = (current == null) ? 0 : current + 1;
    methodStore.put(QAPUtils.PARAM_INDEX_KEY, index);

    if (provider != null) {
      qapTest.setParameterization(new QAPParameterization(provider, index));
    }

    String fqcn = extensionContext.getRequiredTestClass().getName();
    String nestedPath = fqcn.substring(fqcn.lastIndexOf('.') + 1);
    String id =
        nestedPath + "#" + extensionContext.getRequiredTestMethod().getName() + "[" + index + "]";
    qapTest.setTestCaseId(id);

    String testId = extensionContext.getUniqueId();
    startFixtureLogCapture(testId);

    try {
      invocation.proceed();
    } finally {
      List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> testLogs = stopFixtureLogCapture(testId);
      attachLogsToTest(extensionContext, testLogs);
    }
  }

  /**
   * Intercepts regular {@code @Test} method execution to capture timing and logs.
   *
   * <p>Called by JUnit when a regular test method executes. Captures logs during test execution and
   * attaches them to the test's lifecycle for inclusion in the final report.
   *
   * @param invocation the test invocation to execute
   * @param invocationContext reflective context for the test method
   * @param extensionContext the extension context for this test
   * @throws Throwable if the intercepted test method throws
   */
  @Override
  public void interceptTestMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    String testId = extensionContext.getUniqueId();
    startFixtureLogCapture(testId);

    try {
      invocation.proceed();
    } finally {
      List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> testLogs = stopFixtureLogCapture(testId);
      attachLogsToTest(extensionContext, testLogs);
    }
  }

  /**
   * Intercepts {@code @BeforeAll} method execution to capture timing, status, failures, and logs.
   *
   * <p>Called by JUnit before the first test in a class executes. Captures:
   *
   * <ul>
   *   <li>Execution timing (start, end, duration in nanoseconds)
   *   <li>Success/failure status
   *   <li>Exception details if method fails
   *   <li>Log output during execution (if log capture enabled)
   * </ul>
   *
   * <p><strong>Failure Tracking:</strong>
   *
   * <p>If {@code @BeforeAll} fails, the exception is tracked in {@code failedInits} map to
   * coordinate error handling with other lifecycle phases. Cleanup occurs in {@link
   * #interceptAfterAllMethod}.
   *
   * <p>Fixtures are attached to class-level metadata via {@link #addFixtureToClass}.
   *
   * @param invocation the BeforeAll invocation to execute
   * @param invocationContext reflective context for the BeforeAll method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted BeforeAll method throws (propagated after capture)
   */
  @Override
  public void interceptBeforeAllMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();

    ExtensionContext.Store classStore = StoreManager.getClassStore(extensionContext);
    classStore.put(FIXTURE_START_TIME_KEY, startTime);
    classStore.put(FIXTURE_START_TIME_NANOS_KEY, startTimeNanos);

    Method fixtureMethod = (Method) invocationContext.getExecutable();
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

      List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs = stopFixtureLogCapture(fixtureId);
      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;

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

  /**
   * Intercepts {@code @BeforeEach} method execution to capture timing, status, failures, and logs.
   *
   * <p>Called by JUnit before each test method executes. Captures execution details and attaches
   * the fixture to the current test's lifecycle.
   *
   * <p><strong>Failure Handling:</strong>
   *
   * <p>If {@code @BeforeEach} fails, the failure is linked to the current test case by setting the
   * test's failure and status to FAILED. The exception is then re-thrown so JUnit skips the test
   * method execution.
   *
   * @param invocation the BeforeEach invocation to execute
   * @param invocationContext reflective context for the BeforeEach method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted BeforeEach method throws (propagated after capture)
   */
  @Override
  public void interceptBeforeEachMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();

    ExtensionContext.Store methodStore = StoreManager.getMethodStore(extensionContext);
    methodStore.put(FIXTURE_START_TIME_KEY, startTime);
    methodStore.put(FIXTURE_START_TIME_NANOS_KEY, startTimeNanos);

    String fixtureId = extensionContext.getUniqueId() + ":beforeEach:" + System.nanoTime();
    startFixtureLogCapture(fixtureId);

    Throwable failure = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      failure = t;
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

      List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs = stopFixtureLogCapture(fixtureId);
      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;
      Method fixtureMethod = (Method) invocationContext.getExecutable();

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

  /**
   * Intercepts {@code @AfterEach} method execution to capture timing, status, failures, and logs.
   *
   * <p>Called by JUnit after each test method executes. Captures execution details and attaches the
   * fixture to the current test's lifecycle.
   *
   * <p>{@code @AfterEach} failures are recorded in the fixture but do not override the test's
   * success/failure status (test result takes precedence).
   *
   * @param invocation the AfterEach invocation to execute
   * @param invocationContext reflective context for the AfterEach method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted AfterEach method throws (propagated after capture)
   */
  @Override
  public void interceptAfterEachMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();

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

      List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs = stopFixtureLogCapture(fixtureId);
      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;
      Method fixtureMethod = (Method) invocationContext.getExecutable();

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

  /**
   * Intercepts {@code @AfterAll} method execution to capture timing, status, failures, and logs.
   *
   * <p>Called by JUnit after all tests in a class complete. Captures execution details and attaches
   * the fixture to class-level metadata.
   *
   * <p><strong>Memory Cleanup:</strong>
   *
   * <p>Removes this context's entry from {@code failedInits} map to prevent memory leaks in
   * long-running test suites.
   *
   * @param invocation the AfterAll invocation to execute
   * @param invocationContext reflective context for the AfterAll method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted AfterAll method throws (propagated after capture)
   */
  @Override
  public void interceptAfterAllMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();

    ExtensionContext.Store classStore = StoreManager.getClassStore(extensionContext);
    classStore.put(FIXTURE_START_TIME_KEY, startTime);
    classStore.put(FIXTURE_START_TIME_NANOS_KEY, startTimeNanos);

    Method fixtureMethod = (Method) invocationContext.getExecutable();
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

      List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs = stopFixtureLogCapture(fixtureId);
      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;

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

      failedInits.remove(extensionContext.getUniqueId());
    }
  }

  /**
   * Attaches captured logs to the test's lifecycle execution.
   *
   * <p>Ensures the TestExecution object exists in the test's lifecycle before attaching logs.
   * Null-safe - handles missing test or lifecycle gracefully by returning early.
   *
   * @param context the extension context for the test
   * @param logs the captured log entries (may be null if capture wasn't enabled or failed)
   */
  private void attachLogsToTest(
      ExtensionContext context, List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs) {
    QAPTest qapTest =
        StoreManager.getMethodStoreData(context, METHOD_DESCRIPTION_KEY, QAPTest.class);

    if (qapTest == null || qapTest.getLifecycle() == null) {
      return;
    }

    if (qapTest.getLifecycle().getTest() == null) {
      qapTest
          .getLifecycle()
          .setTest(new com.mk.fx.qa.qap.junit.model.QAPTestLifecycle.TestExecution());
    }

    qapTest.getLifecycle().getTest().setLogEntries(logs);
  }

  /**
   * Extracts the parameterization provider name from method annotations.
   *
   * <p>Checks for common JUnit 5 parameterization annotations in priority order:
   *
   * <ol>
   *   <li>CsvSource, MethodSource, ValueSource, ArgumentsSource
   *   <li>EnumSource
   *   <li>NullSource, EmptySource, NullAndEmptySource
   *   <li>Custom sources (any annotation containing "Source" or "Provider")
   * </ol>
   *
   * @param method the test method to examine
   * @return the parameter source name, or null if not a parameterized test
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

    if (method.getAnnotations().length > 0) {
      for (var annotation : method.getAnnotations()) {
        String annotationName = annotation.annotationType().getSimpleName();
        if (annotationName.contains("Source") || annotationName.contains("Provider")) {
          return annotationName;
        }
      }
    }
    return null;
  }

  /**
   * Adds a test-level fixture ({@code @BeforeEach} or {@code @AfterEach}) to the current test's
   * lifecycle.
   *
   * <p>This links fixtures directly to the test that was running when they executed, allowing for
   * detailed timing analysis of setup/teardown overhead vs. actual test execution.
   *
   * <p><strong>Note:</strong> This is only for test-level fixtures. Class-level fixtures
   * ({@code @BeforeAll} and {@code @AfterAll}) are handled by {@link #addFixtureToClass}.
   *
   * @param context the extension context
   * @param phase "BEFORE_EACH" or "AFTER_EACH"
   * @param fixtureMethod the fixture method that executed
   * @param status "PASSED" or "FAILED"
   * @param durationNanos execution duration in nanoseconds
   * @param error the failure details if fixture failed, or null if passed
   * @param logs the captured log entries during fixture execution, or null if unavailable
   */
  private void addFixtureToTest(
      ExtensionContext context,
      String phase,
      Method fixtureMethod,
      String status,
      long durationNanos,
      QAPFailure error,
      List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logs) {
    QAPTest qapTest =
        StoreManager.getMethodStoreData(context, METHOD_DESCRIPTION_KEY, QAPTest.class);
    if (qapTest == null) {
      return;
    }

    if (qapTest.getLifecycle() == null) {
      qapTest.setLifecycle(new com.mk.fx.qa.qap.junit.model.QAPTestLifecycle());
    }

    String methodName = fixtureMethod.getName();
    String className = fixtureMethod.getDeclaringClass().getName();

    int order;
    List<com.mk.fx.qa.qap.junit.model.QAPTestFixture> targetList;
    if ("BEFORE_EACH".equals(phase)) {
      targetList = qapTest.getLifecycle().getBeforeEach();
      order = targetList.size() + 1;
    } else if ("AFTER_EACH".equals(phase)) {
      targetList = qapTest.getLifecycle().getAfterEach();
      order = targetList.size() + 1;
    } else {
      return;
    }

    com.mk.fx.qa.qap.junit.model.QAPTestFixture testFixture =
        new com.mk.fx.qa.qap.junit.model.QAPTestFixture(
            methodName, className, order, status, durationNanos, error, logs);
    targetList.add(testFixture);
  }

  /**
   * Convenience overload for fixtures without log capture.
   *
   * <p>Delegates to the full method with null logs parameter.
   *
   * @param context the extension context
   * @param phase "BEFORE_EACH" or "AFTER_EACH"
   * @param fixtureMethod the fixture method that executed
   * @param status "PASSED" or "FAILED"
   * @param durationNanos execution duration in nanoseconds
   * @param error the failure details if fixture failed, or null if passed
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
   * Adds a class-level fixture ({@code @BeforeAll} or {@code @AfterAll}) to the test class
   * metadata.
   *
   * <p>Uses {@code computeIfAbsent} to safely handle concurrent access to the class nodes map.
   * Thread-safe for parallel test execution.
   *
   * @param context the extension context
   * @param fixture the fixture execution record to add
   * @param fixtureMethod the fixture method (currently unused but kept for potential future use)
   */
  private void addFixtureToClass(
      ExtensionContext context, QAPFixture fixture, Method fixtureMethod) {
    ExtensionContext.Store classStore = StoreManager.getClassStore(context);

    @SuppressWarnings("unchecked")
    Map<String, QAPTestClass> nodes =
        (Map<String, QAPTestClass>)
            classStore.getOrComputeIfAbsent(
                CLASS_NODES_KEY,
                k -> new java.util.concurrent.ConcurrentHashMap<String, QAPTestClass>());

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

    if (testClassNode.getFixtures() == null) {
      testClassNode.setFixtures(new ArrayList<>());
    }

    testClassNode.getFixtures().add(fixture);
  }

  /**
   * Starts log capture for a fixture or test execution.
   *
   * <p>Creates a unique capture ID to isolate fixture/test logs from each other. Uses the same
   * configuration as specified in {@code qap.properties} (or defaults if not configured).
   *
   * <p>Errors during log capture initialization are silently ignored to ensure test execution
   * proceeds.
   *
   * @param fixtureId unique identifier for this capture session
   */
  private void startFixtureLogCapture(String fixtureId) {
    if (logCapturer == null) {
      return;
    }

    try {
      com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig config =
          (logCaptureConfig != null)
              ? logCaptureConfig
              : com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig.defaultConfig();
      logCapturer.startCapture(fixtureId, config);
    } catch (Exception e) {
      // Never fail tests due to logging issues
    }
  }

  /**
   * Stops log capture for a fixture or test execution and returns captured logs.
   *
   * <p>Returns null if capture was not started, logging is disabled, or capture failed. Errors are
   * silently ignored to ensure test execution proceeds.
   *
   * @param fixtureId unique identifier for this capture session
   * @return captured log entries, or null if unavailable
   */
  private List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> stopFixtureLogCapture(String fixtureId) {
    if (logCapturer == null) {
      return null;
    }

    try {
      return logCapturer.stopCapture(fixtureId);
    } catch (Exception e) {
      return null;
    }
  }
}
