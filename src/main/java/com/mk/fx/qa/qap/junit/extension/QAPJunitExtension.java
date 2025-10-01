
package com.mk.fx.qa.qap.junit.extension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.core.QAPLaunchIdGenerator;
import com.mk.fx.qa.qap.junit.core.TestCaseStatus;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPPropertiesLoader;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;
import static com.mk.fx.qa.qap.junit.core.QAPUtils.TEST_CLASS_DATA_KEY;
import static com.mk.fx.qa.qap.junit.core.QAPUtils.buildQAPHeaders;
import static com.mk.fx.qa.qap.junit.core.QAPUtils.isReportingEnabled;


public class QAPJunitExtension
    implements Extension,
        BeforeAllCallback,
        BeforeEachCallback,
        InvocationInterceptor,
        AfterEachCallback,
        AfterAllCallback,
        TestWatcher {

    private final Map<String, Throwable> failedInits = new ConcurrentHashMap<>();
    private final ILifeCycleEventCreator lifeCycleEventCreator =
        new QAPJunitLifeCycleEventCreator(failedInits);
    private final ITestEventCreator eventCreator = new QAPJunitTestEventsCreator();
    private final IMethodInterceptor methodInterceptor = new QAPJunitMethodInterceptor(failedInits);
    private final QAPLaunchIdGenerator launchIdGenerator = new QAPLaunchIdGenerator();

    /**
     * Callback for junit afterAll hook
     *
     * @param context provides test specific information and state
     */
    @Override
    public void afterAll(ExtensionContext context) {
        var launch = StoreManager.getClassStoreData(context, TEST_CLASS_DATA_KEY, QAPJunitLaunch.class);
        // lifeCycleEventCreator.createLifeCycleEvent(LifeCycleEvent.AFTER_ALL, context);
        var qapHeader = launch.getHeader();
        final var qapAttributes = new QAPPropertiesLoader();
        final var gitProperties = qapAttributes.loadGitProperties();
        var git = gitProperties != null ? gitProperties.getProperty("git.branch") : null;
        buildQAPHeaders(qapHeader, git, qapAttributes);
        eventCreator.addTestEventsToTestLaunch(context, launch);
        if (isReportingEnabled(launch, qapAttributes)) {
            //eventPublisher.publishEvent(launch, qapAttributes);
            //Temporary
            ObjectMapper mapper = new ObjectMapper();
            try {
                String s = mapper.writeValueAsString(launch);
                System.out.println(s);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Callback for junit beforeAll hook
     *
     * @param context provides test specific information and state
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        launchIdGenerator.generateLaunchId();
        var qapLaunch = eventCreator.startLaunchQAP(context);
        StoreManager.putClassStoreData(context, TEST_CLASS_DATA_KEY, qapLaunch);
        // lifeCycleEventCreator.createLifeCycleEvent(LifeCycleEvent.BEFORE_ALL, context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var methodName = context.getRequiredTestMethod().getName();
        var rawDisplayName = context.getDisplayName();
        var displayName = resolveDisplayName(context, methodName, rawDisplayName);

        var createTest = new QAPTest(methodName, displayName);
        createTest.setStartTime(Instant.now().toEpochMilli());
        createTest.setTag(context.getTags());

        StoreManager.putMethodStoreData(context, METHOD_DESCRIPTION_KEY, createTest);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        var qapTest = new QAPTest(context.getRequiredTestMethod().getName(), context.getDisplayName());
        qapTest.setStartTime(Instant.now().toEpochMilli());
        qapTest.setTag(context.getTags());
        qapTest.setEndTime(Instant.now().toEpochMilli());
        qapTest.setStatus(TestCaseStatus.DISABLED.name());
        var exc = reason.orElse("Exception not present");
        qapTest.setException(exc.getBytes(StandardCharsets.UTF_8));
        StoreManager.addDescriptionToClassStore(context, qapTest);
    }

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
    public void afterEach(ExtensionContext extensionContext) {
        var qapTest =
                StoreManager.getMethodStoreData(extensionContext, METHOD_DESCRIPTION_KEY, QAPTest.class);
        StoreManager.addDescriptionToClassStore(extensionContext, qapTest);
    }

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

    private String resolveDisplayName(
            ExtensionContext context, String methodName, String rawDisplayName) {
        Optional<Method> method = context.getTestMethod();
        if (method.isEmpty()) {
            return isAutoGeneratedDisplayName(rawDisplayName, methodName) ? methodName : rawDisplayName;
        }

        Method testMethod = method.get();

        DisplayName junitDisplayName = testMethod.getAnnotation(DisplayName.class);
        if (junitDisplayName != null) {
            return junitDisplayName.value();
        }

        DisplayName classDisplayName = testMethod.getDeclaringClass().getAnnotation(DisplayName.class);
        if (classDisplayName != null) {
            return classDisplayName.value();
        }

        if (isAutoGeneratedDisplayName(rawDisplayName, methodName)) {
            return methodName;
        }

        return rawDisplayName;
    }

    private boolean isAutoGeneratedDisplayName(String displayName, String methodName) {
        return displayName.equals(methodName + "()")
                || displayName.startsWith(methodName + "(")
                || displayName.equals(methodName);
    }


}
