package com.mk.fx.qa.qap.junit.extension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.core.QAPLaunchIdGenerator;
import com.mk.fx.qa.qap.junit.core.TestCaseStatus;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPPropertiesLoader;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.*;

/**
 * Bridges JUnit 5 lifecycle with QAP model:
 *  - builds launch & test events
 *  - collects params/logs via interceptors
 *  - serializes and (optionally) publishes results
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

    private final Map<String, Throwable> failedInits;
    private final ILifeCycleEventCreator lifeCycleEventCreator;
    private final ITestEventCreator eventCreator;
    private final IMethodInterceptor methodInterceptor;
    private final QAPLaunchIdGenerator launchIdGenerator;
    private final ObjectMapper objectMapper;
    private final DisplayNameResolver displayNameResolver;

    /**
     * Default constructor for production use.
     * Creates shared state between lifecycle and method interceptors.
     */
    public QAPJunitExtension() {
        ConcurrentHashMap<String, Throwable> sharedFailedInits = new ConcurrentHashMap<>();
        this.failedInits = sharedFailedInits;
        this.lifeCycleEventCreator = new QAPJunitLifeCycleEventCreator(sharedFailedInits);
        this.methodInterceptor = new QAPJunitMethodInterceptor(sharedFailedInits);
        this.eventCreator = new QAPJunitTestEventsCreator();
        this.launchIdGenerator = new QAPLaunchIdGenerator();
        this.objectMapper = new ObjectMapper();
        this.displayNameResolver = new DisplayNameResolver();
    }

    /**
     * Constructor for testing with dependency injection.
     */
    QAPJunitExtension(Map<String, Throwable> failedInits,
                      ILifeCycleEventCreator lifeCycleEventCreator,
                      ITestEventCreator eventCreator,
                      IMethodInterceptor methodInterceptor,
                      QAPLaunchIdGenerator launchIdGenerator,
                      ObjectMapper objectMapper,
                      DisplayNameResolver displayNameResolver) {
        this.failedInits = failedInits;
        this.lifeCycleEventCreator = lifeCycleEventCreator;
        this.eventCreator = eventCreator;
        this.methodInterceptor = methodInterceptor;
        this.launchIdGenerator = launchIdGenerator;
        this.objectMapper = objectMapper;
        this.displayNameResolver = displayNameResolver;
    }

    // ---- JUnit lifecycle ---------------------------------------------------

    @Override
    public void beforeAll(ExtensionContext context) {
        ensureLaunchId();
        if (isTopLevelClassContext(context)) {
            QAPJunitLaunch launch = eventCreator.startLaunchQAP(context);
            StoreManager.putClassStoreData(context, TEST_CLASS_DATA_KEY, launch);
        }
        safeCreateLifeCycleEvent(LifeCycleEvent.BEFORE_ALL, context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        QAPTest qapTest = newQapTest(context);
        qapTest.setStartTime(now());
        // Capture method-level tags only (exclude class/inherited tags)
        qapTest.setTag(extractMethodTags(context));
        // Capture inherited class tags from enclosing classes (excluding current class)
        qapTest.setInheritedClassTags(extractInheritedClassTags(context));
        StoreManager.putMethodStoreData(context, METHOD_DESCRIPTION_KEY, qapTest);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        QAPTest qapTest = StoreManager.getMethodStoreData(context, METHOD_DESCRIPTION_KEY, QAPTest.class);
        StoreManager.addDescriptionToClassStore(context, qapTest);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        QAPJunitLaunch launch = StoreManager.getClassStoreData(context, TEST_CLASS_DATA_KEY, QAPJunitLaunch.class);

        if (!isTopLevelClassContext(context)) {
            // Still capture lifecycle event entries for nested classes, but avoid emitting JSON here
            safeCreateLifeCycleEvent(LifeCycleEvent.AFTER_ALL, context);
            return;
        }

        if (launch == null) {
            log.warn("No launch data found for context: {}. Skipping report generation.",
                    context.getDisplayName());
            return;
        }

        safeCreateLifeCycleEvent(LifeCycleEvent.AFTER_ALL, context);

        QAPPropertiesLoader props = new QAPPropertiesLoader();
        var gitProps = props.loadGitProperties();
        String gitBranch = (gitProps != null) ? gitProps.getProperty("git.branch") : null;
        buildQAPHeaders(launch.getHeader(), gitBranch, props);

        eventCreator.addTestEventsToTestLaunch(context, launch);

        if (isReportingEnabled(launch, props)) {
            serializeAndLogLaunch(launch);
        }
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
        QAPTest qapTest = newQapTest(context);
        qapTest.setStartTime(now());
        // Capture method-level tags only (exclude class/inherited tags)
        qapTest.setTag(extractMethodTags(context));
        // Capture inherited class tags from enclosing classes (excluding current class)
        qapTest.setInheritedClassTags(extractInheritedClassTags(context));
        qapTest.setEndTime(now());
        qapTest.setStatus(TestCaseStatus.DISABLED.name());
        String msg = reason.orElse("Test disabled (no reason provided)");
        qapTest.setException(toBytes(msg));
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
     * Creates a new QAPTest object with all metadata populated from the test context.
     */
    private QAPTest newQapTest(ExtensionContext context) {
        String methodName = context.getRequiredTestMethod().getName();
        String rawDisplay = context.getDisplayName();

        // Run-level display name (parameterized test dynamic names preserved)
        String runDisplay = displayNameResolver.resolveRunDisplayName(context, methodName, rawDisplay);
        QAPTest test = new QAPTest(methodName, runDisplay, null, null, null, null);

        // Static method-level @DisplayName
        test.setMethodDisplayName(displayNameResolver.resolveMethodDisplayName(context));

        // Parent class/nested context display name
        String classDisplayName = displayNameResolver.resolveClassDisplayName(context);
        test.setParentDisplayName(classDisplayName);

        // Parent class key (FQCN)
        test.setParentClassKey(displayNameResolver.resolveParentClassKey(context));

        // Parent chain = ancestors + current class
        List<String> chain = displayNameResolver.buildParentChain(context);
        chain.add(classDisplayName);
        test.setParentChain(chain);

        return test;
    }

    /**
     * Serializes the launch data to JSON and logs it.
     * Errors during serialization are logged but do not fail the test run.
     */
    private void serializeAndLogLaunch(QAPJunitLaunch launch) {
        try {
            String json = objectMapper.writeValueAsString(launch);
            log.info("QAP Launch payload: {}", json);
            System.out.println(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize QAP launch payload", e);
        }
    }

    private long now() {
        return Instant.now().toEpochMilli();
    }

    /**
     * Extracts only the tags declared directly on the test method.
     * Does not include class or enclosing context tags.
     */
    private java.util.Set<String> extractMethodTags(ExtensionContext context) {
        return context.getTestMethod()
                .map(m -> {
                    java.util.Set<String> tags = new java.util.HashSet<>();
                    for (org.junit.jupiter.api.Tag t : m.getAnnotationsByType(org.junit.jupiter.api.Tag.class)) {
                        tags.add(t.value());
                    }
                    return java.util.Collections.unmodifiableSet(tags);
                })
                .orElse(java.util.Collections.emptySet());
    }

    /**
     * Walks up the ExtensionContext parent chain and collects @Tag annotations declared on
     * enclosing classes, excluding the current test's class.
     */
    private java.util.Set<String> extractInheritedClassTags(ExtensionContext context) {
        java.util.Set<String> tags = new java.util.HashSet<>();
        java.util.Optional<Class<?>> current = context.getTestClass();
        java.util.Optional<ExtensionContext> parent = context.getParent();
        while (parent.isPresent()) {
            ExtensionContext p = parent.get();
            java.util.Optional<Class<?>> cls = p.getTestClass();
            if (cls.isPresent() && (current.isEmpty() || !cls.get().equals(current.get()))) {
                for (org.junit.jupiter.api.Tag t : cls.get().getAnnotationsByType(org.junit.jupiter.api.Tag.class)) {
                    tags.add(t.value());
                }
            }
            parent = p.getParent();
        }
        return java.util.Collections.unmodifiableSet(tags);
    }

    /**
     * Ensures a launch ID exists, generating one if necessary.
     * Thread-safe - the LaunchIdGenerator handles concurrency internally.
     */
    private void ensureLaunchId() {
        String id = launchIdGenerator.getLaunchId();
        if (id == null || id.isBlank()) {
             launchIdGenerator.generateLaunchId();
        }
    }

    /**
     * Safely executes lifecycle event creation, logging failures as debug messages.
     * Lifecycle event failures should not break the test run.
     */
    private void safeCreateLifeCycleEvent(LifeCycleEvent event, ExtensionContext ctx) {
        try {
            lifeCycleEventCreator.createLifeCycleEvent(event, ctx);
        } catch (Exception e) {
            log.debug("Lifecycle event '{}' failed (non-fatal): {}", event, e.getMessage(), e);
        }
    }

    /**
     * Converts a string to UTF-8 bytes.
     */
    private byte[] toBytes(String text) {
        if (text == null) {
            return new byte[0];
        }
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Converts a throwable's stack trace to UTF-8 bytes.
     */
    private byte[] toBytes(Throwable throwable) {
        if (throwable == null) {
            return new byte[0];
        }
        return toBytes(stackTraceOf(throwable));
    }

    /**
     * Extracts the full stack trace from a throwable as a string.
     */
    private String stackTraceOf(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private boolean isTopLevelClassContext(ExtensionContext context) {
        Class<?> current = context.getRequiredTestClass();
        Class<?> top = com.mk.fx.qa.qap.junit.store.StoreManager.resolveTopLevelTestClass(context);
        return current.equals(top);
    }
}
