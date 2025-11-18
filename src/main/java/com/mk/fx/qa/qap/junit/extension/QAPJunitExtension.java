package com.mk.fx.qa.qap.junit.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.core.QAPLaunchIdGenerator;
import com.mk.fx.qa.qap.junit.core.TestCaseStatus;
import com.mk.fx.qa.qap.junit.core.QAPUtils;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPPropertiesLoader;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.runtime.QAPRuntime;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import com.mk.fx.qa.qap.junit.util.ExceptionFormatter;
import com.mk.fx.qa.qap.junit.util.TagExtractor;
import com.mk.fx.qa.qap.junit.factory.TestMetadataFactory;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

/**
 * Bridges JUnit 5 lifecycle with the QAP reporting model.
 *
 * Responsibilities
 *  - Translate JUnit lifecycle callbacks into QAP launch/test events
 *  - Collect per-test metadata, tags, and nested-parent hierarchy
 *  - Aggregate nested test classes under a single top-level launch
 *  - Serialize and publish results via a pluggable LaunchPublisher strategy
 *
 * Thread-safety & Concurrency
 *  - Launch ID generation uses a synchronized generator to avoid races
 *  - Shared state across callbacks leverages JUnit ExtensionContext.Store
 *    keyed to the test root; nested classes share the top-level class store
 *  - Lifecycle errors do not fail the test run; they are logged at WARN
 *
 * Extensibility
 *  - Collaborators (ObjectMapper, Clock, Properties, Publisher, Resolver) are
 *    provided via QAPRuntime, enabling easy substitution in tests and runtime
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

    private final ILifeCycleEventCreator lifeCycleEventCreator;
    private final ITestEventCreator eventCreator;
    private final IMethodInterceptor methodInterceptor;
    private final QAPLaunchIdGenerator launchIdGenerator;
    private final ObjectMapper objectMapper;
    private final DisplayNameResolver displayNameResolver;
    private final QAPRuntime runtime;

    /**
     * Default constructor for production use.
     * Creates shared state between lifecycle and method interceptors.
     */
    public QAPJunitExtension() {
        ConcurrentHashMap<String, Throwable> sharedFailedInits = new ConcurrentHashMap<>();
        QAPRuntime rt = QAPRuntime.defaultRuntime();
        ILifeCycleEventCreator lcec = new QAPJunitLifeCycleEventCreator(sharedFailedInits);
        IMethodInterceptor mi = new QAPJunitMethodInterceptor(sharedFailedInits);
        ITestEventCreator tec = new QAPJunitTestEventsCreator();
        QAPLaunchIdGenerator gen = new QAPLaunchIdGenerator();
        this.runtime = Objects.requireNonNull(rt, "runtime");
        this.lifeCycleEventCreator = lcec;
        this.eventCreator = tec;
        this.methodInterceptor = mi;
        this.launchIdGenerator = gen;
        this.objectMapper = runtime.getObjectMapper();
        this.displayNameResolver = runtime.getDisplayNameResolver();
    }

    /**
     * Test constructor: injects runtime and all collaborators from a single source.
     */
    public QAPJunitExtension(QAPRuntime runtime,
                             ILifeCycleEventCreator lifeCycleEventCreator,
                             ITestEventCreator eventCreator,
                             IMethodInterceptor methodInterceptor,
                             QAPLaunchIdGenerator launchIdGenerator) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.lifeCycleEventCreator = Objects.requireNonNull(lifeCycleEventCreator, "lifeCycleEventCreator");
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
        if (isTopLevelClassContext(context)) {
            QAPJunitLaunch launch = eventCreator.startLaunchQAP(context);
            StoreManager.putClassStoreData(context, QAPUtils.TEST_CLASS_DATA_KEY, launch);
        }
        safeCreateLifeCycleEvent(LifeCycleEvent.BEFORE_ALL, context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        QAPTest qapTest = initializeQAPTest(context);
        StoreManager.putMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, qapTest);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        QAPTest qapTest = StoreManager.getMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, QAPTest.class);
        StoreManager.addDescriptionToClassStore(context, qapTest);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        QAPJunitLaunch launch = StoreManager.getClassStoreData(context, QAPUtils.TEST_CLASS_DATA_KEY, QAPJunitLaunch.class);

        if (!isTopLevelClassContext(context)) {
            safeCreateLifeCycleEvent(LifeCycleEvent.AFTER_ALL, context);
            return;
        }

        // Attempt recovery if launch is missing at top-level
        if (launch == null) {
            log.warn("No launch found for top-level context '{}' (launchId='{}'), attempting recovery.",
                    context.getDisplayName(), launchIdGenerator.getLaunchId());
            launch = eventCreator.startLaunchQAP(context);
            StoreManager.putClassStoreData(context, QAPUtils.TEST_CLASS_DATA_KEY, launch);
        }

        safeCreateLifeCycleEvent(LifeCycleEvent.AFTER_ALL, context);

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
    public void interceptBeforeAllMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> invocationContext,
                                         ExtensionContext extensionContext) throws Throwable {
        methodInterceptor.interceptBeforeAllMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext) throws Throwable {
        methodInterceptor.interceptTestTemplateMethod(invocation, invocationContext, extensionContext);
    }

    // ---- helpers -----------------------------------------------------------

    /**
     * Serializes the launch data to JSON and logs it.
     * Errors during serialization are logged but do not fail the test run.
     */
    private void publishLaunch(QAPJunitLaunch launch) {
        runtime.getLaunchPublisher().publish(launch, objectMapper, log);
    }

    private long now() {
        return runtime.getClock().instant().toEpochMilli();
    }

    /**
     * Ensures a launch ID exists, generating one if necessary.
     * Thread-safe - the LaunchIdGenerator handles concurrency internally.
     */
    private void ensureLaunchId() {
        launchIdGenerator.generateIfAbsent();
    }

    /**
     * Safely executes lifecycle event creation, logging failures as debug messages.
     * Lifecycle event failures should not break the test run.
     */
    private void safeCreateLifeCycleEvent(LifeCycleEvent event, ExtensionContext ctx) {
        try {
            lifeCycleEventCreator.createLifeCycleEvent(event, ctx);
        } catch (RuntimeException e) {
            log.warn("Lifecycle event '{}' failed (non-fatal) for context '{}' (launchId='{}'): {}",
                    event, ctx.getDisplayName(), launchIdGenerator.getLaunchId(), e.getMessage(), e);
        }
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
            log.info("Reporting disabled. Skipping launch publish for '{}' (launchId='{}').",
                    context.getDisplayName(), launch.getHeader().getLaunchId());
        }
    }

    /**
     * Creates and initializes a QAPTest from the context: metadata, start time, and tags.
     */
    private QAPTest initializeQAPTest(ExtensionContext context) {
        QAPTest qapTest = TestMetadataFactory.create(context, displayNameResolver);
        qapTest.setStartTime(now());
        qapTest.setTag(TagExtractor.methodTags(context));
        // Class-level tags are now attached to QAPTestClass; only method tags remain on test
        // Pre-populate a stable testCaseId without index; parameterized runs will overwrite with [index]
        Class<?> top = StoreManager.resolveTopLevelTestClass(context);
        String topSimple = (top != null) ? top.getSimpleName() : context.getRequiredTestClass().getSimpleName();
        String id = topSimple + "#" + context.getRequiredTestMethod().getName();
        qapTest.setTestCaseId(id);
        return qapTest;
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
