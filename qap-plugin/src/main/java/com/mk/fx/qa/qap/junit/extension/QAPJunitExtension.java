package com.mk.fx.qa.qap.junit.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.core.QAPLaunchIdGenerator;
import com.mk.fx.qa.qap.junit.core.QAPUtils;
import com.mk.fx.qa.qap.junit.core.TestCaseStatus;
import com.mk.fx.qa.qap.junit.factory.TestMetadataFactory;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPPropertiesLoader;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.runtime.QAPRuntime;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import com.mk.fx.qa.qap.junit.util.CoverageItemExtractor;
import com.mk.fx.qa.qap.junit.util.TagExtractor;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges JUnit 5 lifecycle with the QAP reporting model.
 *
 * <p>This extension automatically captures comprehensive test execution data including timing,
 * results, nested class hierarchies, parameterized test variations, and detailed logging output. It
 * requires no configuration beyond the {@code @ExtendWith} annotation:
 *
 * <pre>{@code
 * @ExtendWith(QAPJunitExtension.class)
 * class MyTest {
 *     @Test
 *     void testSomething() {
 *         // Test execution is automatically captured
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Responsibilities:</strong>
 *
 * <ul>
 *   <li>Translate JUnit lifecycle callbacks into QAP launch/test events
 *   <li>Collect per-test metadata, tags, and nested-parent hierarchy
 *   <li>Aggregate nested test classes under a single top-level launch
 *   <li>Serialize and publish results via a pluggable LaunchPublisher strategy
 *   <li>Coordinate log capture through QAPLogCaptureManager
 * </ul>
 *
 * <p><strong>Thread-safety &amp; Concurrency:</strong>
 *
 * <ul>
 *   <li>Launch ID generation uses a synchronized generator to avoid races
 *   <li>Shared state across callbacks leverages JUnit ExtensionContext.Store keyed to the test
 *       root; nested classes share the top-level class store
 *   <li>{@code SharedInterceptorState} provides thread-safe coordination between extension
 *       lifecycle and method interceptors
 *   <li>Display names are cached per-context to avoid redundant resolution
 *   <li>Lifecycle errors do not fail the test run; they are logged at WARN level
 * </ul>
 *
 * <p><strong>Extensibility:</strong>
 *
 * <ul>
 *   <li>Collaborators (ObjectMapper, Clock, Properties, Publisher, Resolver) are provided via
 *       QAPRuntime, enabling easy substitution in tests and runtime
 *   <li>Log capture is managed by QAPLogCaptureManager, which auto-discovers available logging
 *       frameworks (Log4j2, Logback) via ServiceLoader
 *   <li>Method interceptors receive runtime configuration for flexible behavior customization
 * </ul>
 *
 * <p><strong>Architecture:</strong>
 *
 * <ul>
 *   <li>{@code SharedInterceptorState}: Coordinates shared state (failed initializations) between
 *       extension lifecycle methods and method interceptors
 *   <li>{@link com.mk.fx.qa.qap.junit.logging.QAPLogCaptureManager}: Manages log capture lifecycle,
 *       initialization, and configuration
 *   <li>Display name resolution is cached at the context level for performance
 *   <li>Timestamps are captured once per operation for consistency
 * </ul>
 *
 * @see QAPRuntime
 * @see com.mk.fx.qa.qap.junit.logging.QAPLogCaptureManager
 * @since 1.0
 */
public class QAPJunitExtension
    implements Extension,
        BeforeAllCallback,
        BeforeEachCallback,
        InvocationInterceptor,
        AfterEachCallback,
        AfterAllCallback,
        TestWatcher {

  private static final Logger log = LoggerFactory.getLogger(QAPJunitExtension.class);

  // Cache key for display name resolution
  private static final String DISPLAY_NAME_CACHE_KEY = "qap.displayName.cache";

  /** Shared state between extension lifecycle and method interceptors. */
  private static class SharedInterceptorState {
    private final ConcurrentHashMap<String, Throwable> failedInits = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Throwable> getFailedInits() {
      return failedInits;
    }
  }

  private final ITestEventCreator eventCreator;
  private final IMethodInterceptor methodInterceptor;
  private final QAPLaunchIdGenerator launchIdGenerator;
  private final ObjectMapper objectMapper;
  private final DisplayNameResolver displayNameResolver;
  private final QAPRuntime runtime;
  private final SharedInterceptorState sharedState;
  private final com.mk.fx.qa.qap.junit.logging.QAPLogCaptureManager logCaptureManager;

  /**
   * Default constructor for production use. Creates shared state between lifecycle and method
   * interceptors.
   */
  public QAPJunitExtension() {
    this.runtime = QAPRuntime.defaultRuntime();
    this.sharedState = new SharedInterceptorState();
    this.methodInterceptor = new QAPJunitMethodInterceptor(sharedState.getFailedInits());
    this.eventCreator = new QAPJunitTestEventsCreator();
    this.launchIdGenerator = new QAPLaunchIdGenerator();
    this.objectMapper = runtime.getObjectMapper();
    this.displayNameResolver = runtime.getDisplayNameResolver();
    this.logCaptureManager = new com.mk.fx.qa.qap.junit.logging.QAPLogCaptureManager(runtime);

    log.debug("QAPJunitExtension initialized");
  }

  /** Test constructor: injects runtime and all collaborators from a single source. */
  public QAPJunitExtension(
      QAPRuntime runtime,
      ITestEventCreator eventCreator,
      IMethodInterceptor methodInterceptor,
      QAPLaunchIdGenerator launchIdGenerator) {
    this.runtime = Objects.requireNonNull(runtime, "runtime");
    this.sharedState = new SharedInterceptorState();
    this.eventCreator = Objects.requireNonNull(eventCreator, "eventCreator");
    this.methodInterceptor = Objects.requireNonNull(methodInterceptor, "methodInterceptor");
    this.launchIdGenerator = Objects.requireNonNull(launchIdGenerator, "launchIdGenerator");
    this.objectMapper = this.runtime.getObjectMapper();
    this.displayNameResolver = this.runtime.getDisplayNameResolver();
    this.logCaptureManager = new com.mk.fx.qa.qap.junit.logging.QAPLogCaptureManager(runtime);
  }

  // ---- JUnit lifecycle ---------------------------------------------------

  /**
   * Initializes the QAP launch and class-level tracking.
   *
   * <p>Called by JUnit before any tests in a class execute. For top-level test classes, this
   * creates a new launch and initializes log capture. For all classes (including nested), this
   * registers the class node for hierarchy tracking.
   *
   * <p>Launch ID generation is idempotent - the same ID is used across all test classes in a single
   * test run.
   *
   * @param context the extension context for the test class
   */
  @Override
  public void beforeAll(ExtensionContext context) {
    log.debug("beforeAll called for: {}", context.getDisplayName());
    ensureLaunchId();
    // Start launch only once at top-level
    if (isTopLevelClassContext(context)) {
      log.debug("Starting QAP launch for top-level context: {}", context.getDisplayName());
      QAPJunitLaunch launch = eventCreator.startLaunchQAP(context);
      StoreManager.putClassStoreData(context, QAPUtils.TEST_CLASS_DATA_KEY, launch);

      // Initialize log capture (only once at top level)
      initializeLogCapture();
    }
    // Always register class node and record lifecycle for current class (supports nested)
    registerClassNode(context);
  }

  /**
   * Initializes test-level tracking before each test method execution.
   *
   * <p>Called by JUnit before each test method. Creates a {@link QAPTest} object with metadata,
   * timestamps, tags, and an initialized lifecycle tracker. Stores this in the method-level context
   * for access during and after test execution.
   *
   * <p>Note: Log capture for the test is managed by the method interceptor, not here.
   *
   * @param context the extension context for the test method
   */
  @Override
  public void beforeEach(ExtensionContext context) {
    QAPTest qapTest = initializeQAPTest(context);
    Objects.requireNonNull(qapTest, "QAPTest initialization failed");
    StoreManager.putMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, qapTest);
    // Note: Log capture is managed by the method interceptor
  }

  /**
   * Finalizes test-level tracking after each test method execution.
   *
   * <p>Called by JUnit after each test method. Retrieves the {@link QAPTest} object and adds it to
   * the class-level store for aggregation into the final launch report.
   *
   * <p>Note: Log capture is managed by the method interceptor and is already attached to the test
   * at this point.
   *
   * @param context the extension context for the test method
   */
  @Override
  public void afterEach(ExtensionContext context) {
    QAPTest qapTest =
        StoreManager.getMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, QAPTest.class);
    StoreManager.addDescriptionToClassStore(context, qapTest);
  }

  /**
   * Finalizes the QAP launch and publishes results.
   *
   * <p>Called by JUnit after all tests in a class complete. For top-level test classes, this
   * aggregates all test data, builds the final launch report, and publishes it if reporting is
   * enabled. For nested classes, this simply records lifecycle information.
   *
   * <p>If the launch object is missing (indicating a potential failure in {@link #beforeAll}), this
   * method attempts recovery by creating a new launch. If recovery fails, the method returns early
   * to prevent NPE.
   *
   * @param context the extension context for the test class
   */
  @Override
  public void afterAll(ExtensionContext context) {
    QAPJunitLaunch launch =
        StoreManager.getClassStoreData(context, QAPUtils.TEST_CLASS_DATA_KEY, QAPJunitLaunch.class);
    if (!isTopLevelClassContext(context)) {
      // Record nested class lifecycle but do not finalize launch here
      return;
    }

    // Attempt recovery if launch is missing at top-level
    if (launch == null) {
      log.warn(
          "No launch found for top-level context '{}', attempting recovery.",
          context.getDisplayName());
      try {
        launch = eventCreator.startLaunchQAP(context);
        if (launch == null) {
          log.error(
              "Launch recovery failed - unable to create launch for context: {}",
              context.getDisplayName());
          return;
        }
        StoreManager.putClassStoreData(context, QAPUtils.TEST_CLASS_DATA_KEY, launch);
        log.info("Launch recovery successful for: {}", context.getDisplayName());
      } catch (Exception e) {
        log.error("Launch recovery failed with exception for: {}", context.getDisplayName(), e);
        return;
      }
    }

    finalizeLaunch(context, launch);
  }

  // ---- TestWatcher -------------------------------------------------------

  /**
   * Records a successful test execution.
   *
   * <p>Called by JUnit when a test completes successfully. Creates a test template with PASSED
   * status and adds it to the current test launch.
   *
   * @param context the extension context for the test
   */
  @Override
  public void testSuccessful(ExtensionContext context) {
    eventCreator.createTestTemplate(context, TestCaseStatus.PASSED, null);
  }

  /**
   * Records a test that was aborted during execution.
   *
   * <p>Called by JUnit when a test is aborted (typically via {@code Assumptions.assumeTrue} failing
   * or programmatic abortion). Creates a test template with ABORTED status.
   *
   * @param context the extension context for the test
   * @param cause the throwable that caused the abortion
   */
  @Override
  public void testAborted(ExtensionContext context, Throwable cause) {
    eventCreator.createTestTemplate(context, TestCaseStatus.ABORTED, cause);
  }

  /**
   * Records a failed test execution.
   *
   * <p>Called by JUnit when a test fails due to assertion failures or unexpected exceptions.
   * Creates a test template with FAILED status and captures the failure cause.
   *
   * @param context the extension context for the test
   * @param cause the throwable that caused the failure
   */
  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    eventCreator.createTestTemplate(context, TestCaseStatus.FAILED, cause);
  }

  /**
   * Records a disabled test.
   *
   * <p>Called by JUnit when a test is disabled via {@code @Disabled} annotation. Creates a test
   * record with DISABLED status and captures the reason if provided. No failure is recorded since
   * the test was not executed.
   *
   * @param context the extension context for the test
   * @param reason optional reason why the test is disabled
   */
  @Override
  public void testDisabled(ExtensionContext context, Optional<String> reason) {
    QAPTest qapTest = initializeQAPTest(context);
    qapTest.setEndTime(now());
    qapTest.setEndTimeNanos(nowNanos());
    qapTest.setStatus(TestCaseStatus.DISABLED.name());
    String msg = reason.orElse("Test disabled (no reason provided)");
    qapTest.setDisabledReason(msg);
    qapTest.setFailure(null); // Disabled tests have no failure
    StoreManager.addDescriptionToClassStore(context, qapTest);
  }

  // ---- InvocationInterceptor ---------------------------------------------

  /**
   * Intercepts {@code @BeforeAll} method execution to capture timing and log data.
   *
   * <p>Delegates to the configured method interceptor for detailed execution tracking.
   *
   * @param invocation the invocation to execute
   * @param invocationContext reflective context for the method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted method throws
   */
  @Override
  public void interceptBeforeAllMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptBeforeAllMethod(invocation, invocationContext, extensionContext);
  }

  /**
   * Intercepts {@code @BeforeEach} method execution to capture timing and log data.
   *
   * <p>Delegates to the configured method interceptor for detailed execution tracking.
   *
   * @param invocation the invocation to execute
   * @param invocationContext reflective context for the method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted method throws
   */
  @Override
  public void interceptBeforeEachMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptBeforeEachMethod(invocation, invocationContext, extensionContext);
  }

  /**
   * Intercepts regular {@code @Test} method execution to capture timing and log data.
   *
   * <p>Delegates to the configured method interceptor for detailed execution tracking.
   *
   * @param invocation the invocation to execute
   * @param invocationContext reflective context for the method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted method throws
   */
  @Override
  public void interceptTestMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptTestMethod(invocation, invocationContext, extensionContext);
  }

  /**
   * Intercepts {@code @AfterEach} method execution to capture timing and log data.
   *
   * <p>Delegates to the configured method interceptor for detailed execution tracking.
   *
   * @param invocation the invocation to execute
   * @param invocationContext reflective context for the method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted method throws
   */
  @Override
  public void interceptAfterEachMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptAfterEachMethod(invocation, invocationContext, extensionContext);
  }

  /**
   * Intercepts {@code @AfterAll} method execution to capture timing and log data.
   *
   * <p>Delegates to the configured method interceptor for detailed execution tracking.
   *
   * @param invocation the invocation to execute
   * @param invocationContext reflective context for the method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted method throws
   */
  @Override
  public void interceptAfterAllMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptAfterAllMethod(invocation, invocationContext, extensionContext);
  }

  /**
   * Intercepts parameterized {@code @ParameterizedTest} template method execution.
   *
   * <p>Delegates to the configured method interceptor for detailed execution tracking of each
   * parameterized test invocation.
   *
   * @param invocation the invocation to execute
   * @param invocationContext reflective context for the method
   * @param extensionContext the extension context
   * @throws Throwable if the intercepted method throws
   */
  @Override
  public void interceptTestTemplateMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptTestTemplateMethod(invocation, invocationContext, extensionContext);
  }

  // ---- helpers -----------------------------------------------------------

  /**
   * Serializes the launch data to JSON and logs it.
   *
   * <p>Errors during serialization are logged but do not fail the test run.
   *
   * @param launch the launch data to publish
   */
  private void publishLaunch(QAPJunitLaunch launch) {
    runtime.getLaunchPublisher().publish(launch, objectMapper, log);
  }

  /**
   * Returns the current time in milliseconds since epoch.
   *
   * <p>Uses the configured clock from {@link QAPRuntime}, enabling time control in tests.
   *
   * @return current time in milliseconds
   */
  private long now() {
    return runtime.getClock().instant().toEpochMilli();
  }

  /**
   * Returns the current time in nanoseconds from an arbitrary origin.
   *
   * <p>Used for high-precision duration measurements. The value is only meaningful when compared to
   * other values from this method.
   *
   * @return current time in nanoseconds
   */
  private long nowNanos() {
    return System.nanoTime();
  }

  /**
   * Ensures a unique launch ID exists for this test run.
   *
   * <p>Thread-safe - the generator handles concurrent access internally. Multiple calls are
   * idempotent and return the same ID.
   */
  private void ensureLaunchId() {
    launchIdGenerator.generateIfAbsent();
  }

  /**
   * Aggregates test results and publishes the final launch report.
   *
   * <p>Builds the complete launch headers with environment info, collects all test results, and
   * publishes the report if reporting is enabled in configuration.
   *
   * @param context the extension context for the test class
   * @param launch the launch object to finalize
   */
  private void finalizeLaunch(ExtensionContext context, QAPJunitLaunch launch) {
    QAPPropertiesLoader props = runtime.getPropertiesLoader();
    var gitProps = props.loadGitProperties();
    String gitBranch = (gitProps != null) ? gitProps.getProperty("git.branch") : null;
    QAPUtils.buildQAPHeaders(launch.getHeader(), gitBranch, props);

    eventCreator.addTestEventsToTestLaunch(context, launch);
    if (QAPUtils.isReportingEnabled(launch, props)) {
      publishLaunch(launch);
    } else {
      log.info(
          "Reporting disabled. Skipping launch publish for '{}' (launchId='{}').",
          context.getDisplayName(),
          launch.getHeader().getLaunchId());
    }
  }

  /**
   * Creates and initializes a QAPTest from the context with metadata, start time, and tags.
   *
   * <p>Registers the class node (if not already present) and generates a stable test case ID in the
   * format {@code NestedClassName#methodName}. For parameterized tests, the ID is later updated
   * with {@code [index]} by the method interceptor.
   *
   * @param context the extension context for the test method
   * @return initialized QAPTest object with lifecycle, timestamps, and metadata
   */
  private QAPTest initializeQAPTest(ExtensionContext context) {
    QAPTest qapTest = TestMetadataFactory.create(context, displayNameResolver);

    // Capture both timestamp formats together to ensure consistency
    long startTimeNanos = nowNanos();
    long startTimeMillis = now();

    qapTest.setLifecycle(new com.mk.fx.qa.qap.junit.model.QAPTestLifecycle());
    qapTest.setStartTime(startTimeMillis);
    qapTest.setStartTimeNanos(startTimeNanos);
    qapTest.setTag(TagExtractor.methodTags(context));
    qapTest.setClassTags(TagExtractor.classTags(context));
    qapTest.setInheritedClassTags(TagExtractor.inheritedClassTags(context));
    qapTest.setCoverageItems(CoverageItemExtractor.methodCoverageItems(context));
    qapTest.setTestType("TEST");

    registerClassNode(context);

    // Generate test case ID using nested class path to avoid collisions
    // Format: ClassName$NestedClass#methodName (e.g., "PaymentTest$RefundTests#testFullRefund")
    // Parameterized tests will append [index] later
    String fqcn = context.getRequiredTestClass().getName();
    String nestedPath = fqcn.substring(fqcn.lastIndexOf('.') + 1);
    String id = nestedPath + "#" + context.getRequiredTestMethod().getName();
    qapTest.setTestCaseId(id);

    return qapTest;
  }

  /**
   * Gets or resolves the display name for a class, caching it in the context store to avoid
   * redundant resolution calls.
   *
   * @param context the extension context
   * @return the cached or newly resolved display name
   */
  private String getOrResolveDisplayName(ExtensionContext context) {
    var classStore = StoreManager.getClassStore(context);
    String cached = classStore.get(DISPLAY_NAME_CACHE_KEY, String.class);

    if (cached == null) {
      cached = displayNameResolver.resolveClassDisplayName(context);
      classStore.put(DISPLAY_NAME_CACHE_KEY, cached);
    }

    return cached;
  }

  /**
   * Creates or refreshes the QAPTestClass node metadata for the current class context.
   *
   * <p>Stores class metadata in the class-level store map. For new classes, initializes all
   * metadata including display name, FQN, tags, parent chain, and fixtures list. For existing
   * classes, refreshes only dynamic properties like inherited tags and display name.
   *
   * <p>The nodes map is initialized once via {@code getOrComputeIfAbsent} and subsequent
   * modifications update the stored reference directly.
   *
   * @param context the extension context for the test class
   */
  private void registerClassNode(ExtensionContext context) {
    var classStore = StoreManager.getClassStore(context);

    @SuppressWarnings("unchecked")
    java.util.Map<String, com.mk.fx.qa.qap.junit.model.QAPTestClass> nodes =
        (java.util.Map<String, com.mk.fx.qa.qap.junit.model.QAPTestClass>)
            classStore.getOrComputeIfAbsent(
                com.mk.fx.qa.qap.junit.core.QAPUtils.CLASS_NODES_KEY,
                k ->
                    new java.util.concurrent.ConcurrentHashMap<
                        String, com.mk.fx.qa.qap.junit.model.QAPTestClass>());

    Class<?> cls = context.getRequiredTestClass();
    String key = cls.getName();
    com.mk.fx.qa.qap.junit.model.QAPTestClass node = nodes.get(key);

    if (node == null) {
      String displayName = getOrResolveDisplayName(context);
      node =
          new com.mk.fx.qa.qap.junit.model.QAPTestClass(
              cls.getSimpleName(), displayName, TagExtractor.classTags(context));
      String fqcn = cls.getName();
      node.setClassFqn(fqcn);
      node.setClassSimpleName(cls.getSimpleName());
      node.setInheritedClassTags(TagExtractor.inheritedClassTags(context));
      node.setClassKey(key);
      java.util.List<String> chain = displayNameResolver.buildParentChain(context);
      chain.add(displayName);
      node.setClassChain(chain);
      node.setFixtures(new java.util.ArrayList<>());
      nodes.put(key, node);
    } else {
      // Refresh display name only if changed (rare for static @DisplayName)
      String currentDisplayName = getOrResolveDisplayName(context);
      if (!currentDisplayName.equals(node.getDisplayName())) {
        node.setDisplayName(currentDisplayName);
      }

      node.setInheritedClassTags(TagExtractor.inheritedClassTags(context));

      if (node.getClassChain() == null || node.getClassChain().isEmpty()) {
        java.util.List<String> chain = displayNameResolver.buildParentChain(context);
        chain.add(currentDisplayName);
        node.setClassChain(chain);
      }

      nodes.put(key, node);
      // Map is already in store via getOrComputeIfAbsent - no need to put again
    }
  }

  /**
   * Determines if the given context represents a top-level test class.
   *
   * <p>Top-level classes are responsible for creating launches and publishing results. Nested test
   * classes contribute data to their parent's launch but do not create their own.
   *
   * @param context the extension context to check
   * @return {@code true} if this is a top-level test class, {@code false} for nested classes
   */
  private boolean isTopLevelClassContext(ExtensionContext context) {
    return context
        .getTestClass()
        .map(
            currentClass -> {
              Class<?> topLevelClass =
                  com.mk.fx.qa.qap.junit.store.StoreManager.resolveTopLevelTestClass(context);
              return topLevelClass != null && currentClass.equals(topLevelClass);
            })
        .orElse(false);
  }

  // ---- Logging Capture Support -------------------------------------------

  /**
   * Initializes log capture if a logging implementation is available.
   *
   * <p>Called once at top-level {@link #beforeAll}. Uses ServiceLoader to auto-detect Log4j2 or
   * Logback implementations. Also initializes stack trace configuration from {@code
   * qap.properties}.
   *
   * <p>If initialization fails, the error is logged but does not prevent test execution. The
   * extension will continue without log capture.
   */
  private void initializeLogCapture() {
    try {
      // Initialize stack trace configuration from properties
      initializeStackTraceConfig();

      // Delegate log capture initialization to manager
      logCaptureManager.initialize();

      // Pass capturer and config to method interceptor for fixture log capture
      Optional<com.mk.fx.qa.qap.logging.core.QAPLogCapturer> capturerOpt =
          logCaptureManager.getCapturer();
      Optional<com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig> configOpt =
          logCaptureManager.getConfig();

      if (capturerOpt.isPresent() && configOpt.isPresent()) {
        // Pass to method interceptor for fixture log capture
        if (methodInterceptor instanceof QAPJunitMethodInterceptor) {
          ((QAPJunitMethodInterceptor) methodInterceptor).setLogCapturer(capturerOpt.get());
          ((QAPJunitMethodInterceptor) methodInterceptor).setLogCaptureConfig(configOpt.get());
        }
      }
    } catch (Exception e) {
      // Never fail tests due to logging issues
      log.warn("Failed to initialize log capture: {}", e.getMessage());
      // The logCaptureManager already handles failure state internally
    }
  }

  /**
   * Initializes stack trace configuration from properties.
   *
   * <p>Sets up the {@link com.mk.fx.qa.qap.junit.util.ExceptionFormatter} with user preferences for
   * stack trace capping (max lines, head/tail preservation, framework exit strategy). Uses
   * configuration from {@code qap.properties}.
   */
  private void initializeStackTraceConfig() {
    try {
      QAPPropertiesLoader propertiesLoader = runtime.getPropertiesLoader();
      com.mk.fx.qa.qap.junit.model.StackTraceConfig stackTraceConfig =
          com.mk.fx.qa.qap.junit.model.StackTraceConfig.fromProperties(propertiesLoader);
      com.mk.fx.qa.qap.junit.util.ExceptionFormatter.setStackTraceConfig(stackTraceConfig);
      log.debug(
          "Stack trace config initialized: maxLines={}, headLines={}, tailLines={}",
          stackTraceConfig.getMaxLines(),
          stackTraceConfig.getHeadLines(),
          stackTraceConfig.getTailLines());
    } catch (Exception e) {
      log.warn("Failed to initialize stack trace config, using defaults: {}", e.getMessage());
    }
  }
}
