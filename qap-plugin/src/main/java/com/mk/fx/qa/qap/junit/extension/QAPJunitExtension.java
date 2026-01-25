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
 * <p>Responsibilities - Translate JUnit lifecycle callbacks into QAP launch/test events - Collect
 * per-test metadata, tags, and nested-parent hierarchy - Aggregate nested test classes under a
 * single top-level launch - Serialize and publish results via a pluggable LaunchPublisher strategy
 *
 * <p>Thread-safety & Concurrency - Launch ID generation uses a synchronized generator to avoid
 * races - Shared state across callbacks leverages JUnit ExtensionContext.Store keyed to the test
 * root; nested classes share the top-level class store - Lifecycle errors do not fail the test run;
 * they are logged at WARN
 *
 * <p>Extensibility - Collaborators (ObjectMapper, Clock, Properties, Publisher, Resolver) are
 * provided via QAPRuntime, enabling easy substitution in tests and runtime
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

  private final ITestEventCreator eventCreator;
  private final IMethodInterceptor methodInterceptor;
  private final QAPLaunchIdGenerator launchIdGenerator;
  private final ObjectMapper objectMapper;
  private final DisplayNameResolver displayNameResolver;
  private final QAPRuntime runtime;

  // Logging capture support (optional - only initialized if logging modules available)
  private volatile com.mk.fx.qa.qap.logging.core.QAPLogCapturerRegistry logCapturerRegistry;
  private volatile com.mk.fx.qa.qap.logging.core.QAPLogCapturer logCapturer;

  /**
   * Default constructor for production use. Creates shared state between lifecycle and method
   * interceptors.
   */
  public QAPJunitExtension() {
    ConcurrentHashMap<String, Throwable> sharedFailedInits = new ConcurrentHashMap<>();
    QAPRuntime rt = QAPRuntime.defaultRuntime();
    QAPJunitMethodInterceptor mi = new QAPJunitMethodInterceptor(sharedFailedInits);
    ITestEventCreator tec = new QAPJunitTestEventsCreator();
    QAPLaunchIdGenerator gen = new QAPLaunchIdGenerator();
    this.runtime = Objects.requireNonNull(rt, "runtime");
    this.eventCreator = tec;
    this.methodInterceptor = mi;
    this.launchIdGenerator = gen;
    this.objectMapper = runtime.getObjectMapper();
    this.displayNameResolver = runtime.getDisplayNameResolver();
    log.debug("QAPJunitExtension initialized");
  }

  /** Test constructor: injects runtime and all collaborators from a single source. */
  public QAPJunitExtension(
      QAPRuntime runtime,
      ILifeCycleEventCreator lifeCycleEventCreator,
      ITestEventCreator eventCreator,
      IMethodInterceptor methodInterceptor,
      QAPLaunchIdGenerator launchIdGenerator) {
    this.runtime = Objects.requireNonNull(runtime, "runtime");
    this.eventCreator = Objects.requireNonNull(eventCreator, "eventCreator");
    this.methodInterceptor = Objects.requireNonNull(methodInterceptor, "methodInterceptor");
    this.launchIdGenerator = Objects.requireNonNull(launchIdGenerator, "launchIdGenerator");
    this.objectMapper = this.runtime.getObjectMapper();
    this.displayNameResolver = this.runtime.getDisplayNameResolver();
  }

  // ---- JUnit lifecycle ---------------------------------------------------

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

  @Override
  public void beforeEach(ExtensionContext context) {
    QAPTest qapTest = initializeQAPTest(context);
    Objects.requireNonNull(qapTest, "QAPTest initialization failed");

    // Initialize lifecycle tracking for this test (only if not already present)
    if (qapTest.getLifecycle() == null) {
      qapTest.setLifecycle(new com.mk.fx.qa.qap.junit.model.QAPTestLifecycle());
    }
    StoreManager.putMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, qapTest);

    // NOTE: We do NOT start log capture here anymore
    // Log capture for the test execution will start in interceptTestMethod
    // Log capture for beforeEach happens in the interceptor
  }

  @Override
  public void afterEach(ExtensionContext context) {
    QAPTest qapTest =
        StoreManager.getMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, QAPTest.class);

    // Log capture is now handled in interceptors per-phase
    // No need to stop/attach logs here

    StoreManager.addDescriptionToClassStore(context, qapTest);
  }

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
          "No launch found for top-level context '{}' (launchId='{}'), attempting recovery.",
          context.getDisplayName(),
          launchIdGenerator.getLaunchId());
      launch = eventCreator.startLaunchQAP(context);
      StoreManager.putClassStoreData(context, QAPUtils.TEST_CLASS_DATA_KEY, launch);
    }

    finalizeLaunch(context, launch);
  }

  // ---- TestWatcher -------------------------------------------------------

  @Override
  public void testSuccessful(ExtensionContext context) {
    eventCreator.createTestTemplate(context, TestCaseStatus.PASSED, null);
  }

  @Override
  public void testAborted(ExtensionContext context, Throwable cause) {
    eventCreator.createTestTemplate(context, TestCaseStatus.ABORTED, cause);
  }

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    eventCreator.createTestTemplate(context, TestCaseStatus.FAILED, cause);
  }

  @Override
  public void testDisabled(ExtensionContext context, Optional<String> reason) {
    QAPTest qapTest = initializeQAPTest(context);
    qapTest.setEndTime(now());
    qapTest.setEndTimeNanos(nowNanos());
    qapTest.setStatus(TestCaseStatus.DISABLED.name());
    // For disabled tests, set disabledReason instead of failure
    // hasFailure should be false because the test wasn't run
    String msg = reason.orElse("Test disabled (no reason provided)");
    qapTest.setDisabledReason(msg);
    // Explicitly ensure failure is null and hasFailure returns false
    qapTest.setFailure(null);
    StoreManager.addDescriptionToClassStore(context, qapTest);
  }

  // ---- InvocationInterceptor ---------------------------------------------

  @Override
  public void interceptBeforeAllMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptBeforeAllMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptBeforeEachMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptBeforeEachMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptTestMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptTestMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptAfterEachMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptAfterEachMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptAfterAllMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    methodInterceptor.interceptAfterAllMethod(invocation, invocationContext, extensionContext);
  }

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
   * Serializes the launch data to JSON and logs it. Errors during serialization are logged but do
   * not fail the test run.
   */
  private void publishLaunch(QAPJunitLaunch launch) {
    runtime.getLaunchPublisher().publish(launch, objectMapper, log);
  }

  private long now() {
    return runtime.getClock().instant().toEpochMilli();
  }

  private long nowNanos() {
    return System.nanoTime();
  }

  /**
   * Ensures a launch ID exists, generating one if necessary. Thread-safe - the LaunchIdGenerator
   * handles concurrency internally.
   */
  private void ensureLaunchId() {
    launchIdGenerator.generateIfAbsent();
  }

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

  /** Creates and initializes a QAPTest from the context: metadata, start time, and tags. */
  private QAPTest initializeQAPTest(ExtensionContext context) {
    QAPTest qapTest = TestMetadataFactory.create(context, displayNameResolver);
    qapTest.setStartTime(now());
    qapTest.setStartTimeNanos(nowNanos());
    // Method-level tags only
    qapTest.setTag(TagExtractor.methodTags(context));
    // Include class-level tags and inherited parent-class tags on the test
    qapTest.setClassTags(TagExtractor.classTags(context));
    qapTest.setInheritedClassTags(TagExtractor.inheritedClassTags(context));
    qapTest.setTestType("TEST");
    // Ensure class node exists/updated
    registerClassNode(context);
    // Class-level tags are now attached to QAPTestClass; only method tags remain on test
    // Pre-populate a stable testCaseId without index; parameterized runs will overwrite with
    // [index]
    // Use nested class path (without package) to avoid collisions across nested classes
    String fqcn = context.getRequiredTestClass().getName();
    String nestedPath = fqcn.substring(fqcn.lastIndexOf('.') + 1); // e.g., DemoTest$Group$Inner
    String id = nestedPath + "#" + context.getRequiredTestMethod().getName();
    qapTest.setTestCaseId(id);
    return qapTest;
  }

  /**
   * Creates or refreshes the QAPTestClass node metadata for the current class context and stores it
   * in the class-level store map. Ensures displayName and tags are set and a full class chain is
   * calculated (including current class).
   */
  private void registerClassNode(ExtensionContext context) {
    var classStore = StoreManager.getClassStore(context);
    @SuppressWarnings("unchecked")
    java.util.Map<String, com.mk.fx.qa.qap.junit.model.QAPTestClass> nodes =
        classStore.getOrDefault(
            com.mk.fx.qa.qap.junit.core.QAPUtils.CLASS_NODES_KEY,
            java.util.Map.class,
            new java.util.concurrent.ConcurrentHashMap<>());
    Class<?> cls = context.getRequiredTestClass();
    String key = cls.getName();
    com.mk.fx.qa.qap.junit.model.QAPTestClass node = nodes.get(key);
    if (node == null) {
      node =
          new com.mk.fx.qa.qap.junit.model.QAPTestClass(
              cls.getSimpleName(),
              displayNameResolver.resolveClassDisplayName(context),
              TagExtractor.classTags(context));
      // Store fully qualified name and simple name
      String fqcn = cls.getName();
      node.setClassFqn(fqcn);
      node.setClassSimpleName(cls.getSimpleName());
      node.setInheritedClassTags(TagExtractor.inheritedClassTags(context));
      node.setClassKey(key);
      java.util.List<String> chain = displayNameResolver.buildParentChain(context);
      chain.add(node.getDisplayName());
      node.setClassChain(chain);
      // Initialize fixtures list
      node.setFixtures(new java.util.ArrayList<>());
      nodes.put(key, node);
      classStore.put(com.mk.fx.qa.qap.junit.core.QAPUtils.CLASS_NODES_KEY, nodes);
    } else {
      // Refresh potentially dynamic properties
      node.setDisplayName(displayNameResolver.resolveClassDisplayName(context));
      node.setInheritedClassTags(TagExtractor.inheritedClassTags(context));
      if (node.getClassChain() == null || node.getClassChain().isEmpty()) {
        java.util.List<String> chain = displayNameResolver.buildParentChain(context);
        chain.add(node.getDisplayName());
        node.setClassChain(chain);
      }
      nodes.put(key, node);
      classStore.put(com.mk.fx.qa.qap.junit.core.QAPUtils.CLASS_NODES_KEY, nodes);
    }
  }

  private boolean isTopLevelClassContext(ExtensionContext context) {
    Class<?> current = context.getTestClass().orElse(null);
    if (current == null) {
      return false;
    }
    Class<?> top = com.mk.fx.qa.qap.junit.store.StoreManager.resolveTopLevelTestClass(context);
    return current.equals(top);
  }

  // ---- Logging Capture Support -------------------------------------------

  /**
   * Initializes log capture if a logging implementation is available. Called once at top-level
   * beforeAll. Uses ServiceLoader to auto-detect Log4j2/Logback implementations.
   */
  private void initializeLogCapture() {
    try {
      // Initialize stack trace configuration from properties
      initializeStackTraceConfig();

      // Create registry and discover available capturers
      logCapturerRegistry = new com.mk.fx.qa.qap.logging.core.QAPLogCapturerRegistry();
      logCapturerRegistry.discover();

      // Get the first available capturer (highest priority)
      java.util.Optional<com.mk.fx.qa.qap.logging.core.QAPLogCapturer> capturerOpt =
          logCapturerRegistry.getAvailableCapturer();

      if (capturerOpt.isPresent()) {
        logCapturer = capturerOpt.get();
        log.info(
            "✅ Log capture enabled: {} (priority: {})",
            logCapturer.getFrameworkName(),
            logCapturer.getPriority());

        // Build config from properties
        com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig config = buildLogCaptureConfig();

        // Pass log capturer AND config to method interceptor for fixture log capture
        if (methodInterceptor instanceof QAPJunitMethodInterceptor) {
          ((QAPJunitMethodInterceptor) methodInterceptor).setLogCapturer(logCapturer);
          ((QAPJunitMethodInterceptor) methodInterceptor).setLogCaptureConfig(config);
        }
      } else {
        log.debug("No log capturer available - tests will run without log capture");
      }
    } catch (Exception e) {
      // Never fail tests due to logging issues
      log.warn("Failed to initialize log capture: {}", e.getMessage());
      logCapturer = null;
    }
  }

  /**
   * Initializes stack trace configuration from properties. Sets up the ExceptionFormatter with user
   * preferences for stack trace capping.
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

  /**
   * Starts log capture for a specific test. Called in beforeEach after test initialization.
   *
   * @param context the test execution context
   */
  private void startLogCapture(ExtensionContext context) {
    if (logCapturer == null) {
      return; // No capturer available
    }

    try {
      String testId = context.getUniqueId();
      // Build configuration from properties
      com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig config = buildLogCaptureConfig();

      logCapturer.startCapture(testId, config);
      log.debug("Started log capture for test: {}", testId);
    } catch (Exception e) {
      log.warn("Failed to start log capture for {}: {}", context.getDisplayName(), e.getMessage());
    }
  }

  /**
   * Builds log capture configuration from qap.properties. Users can customize logging behavior by
   * setting properties like: - qap.logging.enabled=true - qap.logging.min.level=DEBUG -
   * qap.logging.max.entries=5000 - qap.logging.max.message.length=20000 -
   * qap.logging.capture.stacktraces=true - qap.logging.include.mdc=true -
   * qap.logging.include.markers=true -
   * qap.logging.logger.patterns=com.myapp.*,org.springframework.*
   *
   * @return configured QAPLogCaptureConfig
   */
  private com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig buildLogCaptureConfig() {
    QAPPropertiesLoader props = runtime.getPropertiesLoader();

    com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig.Builder builder =
        com.mk.fx.qa.qap.logging.core.QAPLogCaptureConfig.builder();

    // Enabled (default: true)
    builder.enabled(props.getBooleanProperty("qap.logging.enabled", true));

    // Min level (default: DEBUG for comprehensive capture)
    String minLevelStr = props.getProperty("qap.logging.min.level", "DEBUG");
    try {
      com.mk.fx.qa.qap.logging.core.QAPLogLevel minLevel =
          com.mk.fx.qa.qap.logging.core.QAPLogLevel.valueOf(minLevelStr.toUpperCase());
      builder.minLevel(minLevel);
    } catch (IllegalArgumentException e) {
      log.warn(
          "Invalid qap.logging.min.level '{}', using DEBUG. Valid values: TRACE, DEBUG, INFO, WARN, ERROR, FATAL",
          minLevelStr);
      builder.minLevel(com.mk.fx.qa.qap.logging.core.QAPLogLevel.DEBUG);
    }

    // Max entries per test (default: 1000)
    builder.maxEntriesPerTest(props.getIntProperty("qap.logging.max.entries", 1000));

    // Max message length (default: 10000)
    builder.maxMessageLength(props.getIntProperty("qap.logging.max.message.length", 10000));

    // Capture stack traces (default: true)
    builder.captureStackTraces(props.getBooleanProperty("qap.logging.capture.stacktraces", true));

    // Include MDC (default: true)
    builder.includeMdc(props.getBooleanProperty("qap.logging.include.mdc", true));

    // Include markers (default: true)
    builder.includeMarkers(props.getBooleanProperty("qap.logging.include.markers", true));

    // Logger patterns (default: empty = capture all)
    String patternsStr = props.getProperty("qap.logging.logger.patterns", "");
    if (!patternsStr.trim().isEmpty()) {
      String[] patterns = patternsStr.split(",");
      for (String pattern : patterns) {
        String trimmed = pattern.trim();
        if (!trimmed.isEmpty()) {
          builder.addLoggerPattern(trimmed);
        }
      }
    }

    return builder.build();
  }

  /**
   * Stops log capture and attaches captured logs to the test root level only. The lifecycle.test
   * logs are captured separately in interceptTestMethod.
   *
   * <p>NOTE: This method is no longer used for log capture since we now capture logs per-phase in
   * the interceptors. Kept for potential future use or can be removed.
   *
   * @param context the test execution context
   * @param qapTest the test object to attach logs to
   */
  @SuppressWarnings("unused")
  private void stopLogCaptureAndAttach(ExtensionContext context, QAPTest qapTest) {
    // No longer used - logs are captured per-phase in interceptors
    // Kept for reference or can be removed in future cleanup
  }
}
