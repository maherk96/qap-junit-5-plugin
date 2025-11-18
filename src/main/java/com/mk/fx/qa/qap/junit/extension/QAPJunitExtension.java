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
import com.mk.fx.qa.qap.junit.util.ExceptionFormatter;
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

  /**
   * Default constructor for production use. Creates shared state between lifecycle and method
   * interceptors.
   */
  public QAPJunitExtension() {
    ConcurrentHashMap<String, Throwable> sharedFailedInits = new ConcurrentHashMap<>();
    QAPRuntime rt = QAPRuntime.defaultRuntime();
    IMethodInterceptor mi = new QAPJunitMethodInterceptor(sharedFailedInits);
    ITestEventCreator tec = new QAPJunitTestEventsCreator();
    QAPLaunchIdGenerator gen = new QAPLaunchIdGenerator();
    this.runtime = Objects.requireNonNull(rt, "runtime");
    this.eventCreator = tec;
    this.methodInterceptor = mi;
    this.launchIdGenerator = gen;
    this.objectMapper = runtime.getObjectMapper();
    this.displayNameResolver = runtime.getDisplayNameResolver();
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
    ensureLaunchId();
    // Start launch only once at top-level
    if (isTopLevelClassContext(context)) {
      QAPJunitLaunch launch = eventCreator.startLaunchQAP(context);
      StoreManager.putClassStoreData(context, QAPUtils.TEST_CLASS_DATA_KEY, launch);
    }
    // Always register class node and record lifecycle for current class (supports nested)
    registerClassNode(context);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    QAPTest qapTest = initializeQAPTest(context);
    StoreManager.putMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, qapTest);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    QAPTest qapTest =
        StoreManager.getMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, QAPTest.class);
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
    qapTest.setStatus(TestCaseStatus.DISABLED.name());
    String msg = reason.orElse("Test disabled (no reason provided)");
    qapTest.setException(ExceptionFormatter.toBytes(msg));
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
      // Store human-readable nested path without package as fullClassName
      String fqcn = cls.getName();
      String nestedPath = fqcn.substring(fqcn.lastIndexOf('.') + 1);
      node.setFullClassName(nestedPath);
      node.setInheritedClassTags(TagExtractor.inheritedClassTags(context));
      node.setClassKey(key);
      java.util.List<String> chain = displayNameResolver.buildParentChain(context);
      chain.add(node.getDisplayName());
      node.setClassChain(chain);
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
}
