package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.core.QAPUtils;
import com.mk.fx.qa.qap.junit.core.TestCaseStatus;
import com.mk.fx.qa.qap.junit.model.QAPHeader;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.model.QAPTestClass;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.TEST_CLASS_DATA_KEY;


public class QAPJunitTestEventsCreator implements ITestEventCreator {

    private static final String SYSTEM_PROPERTY_LAUNCH_ID = "launchID";

    @Override
    public void addTestEventsToTestLaunch(ExtensionContext context, QAPJunitLaunch launch) {
        var events = StoreManager.getClassStoreData(
                context, QAPUtils.METHOD_DESCRIPTION_KEY, List.class);
        launch.getTestClass().setTestCases(events);
    }

    /**
     * @param context provides test specific information and state
     * @param status  test status
     * @param t       exception if present
     */
    @Override
    public void createTestTemplate(ExtensionContext context, TestCaseStatus status, Throwable t) {
        var qapTest = StoreManager.getMethodStoreData(
                context, QAPUtils.METHOD_DESCRIPTION_KEY, QAPTest.class);
        qapTest.setEndTime(Instant.now().toEpochMilli());
        qapTest.setStatus(status.name());
        if (t != null) {
            qapTest.setException(t.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * @param context provides test specific information and state
     * @return launch for junit test
     */
    @Override
    public QAPJunitLaunch startLaunchQAP(ExtensionContext context) {
        var clazz = context.getRequiredTestClass();
        java.util.Set<String> classTags = new java.util.HashSet<>();
        for (org.junit.jupiter.api.Tag t : clazz.getAnnotationsByType(org.junit.jupiter.api.Tag.class)) {
            classTags.add(t.value());
        }
        var qapLaunch = new QAPJunitLaunch(
                new QAPHeader(
                        Instant.now().toEpochMilli(),
                        System.getProperty(SYSTEM_PROPERTY_LAUNCH_ID)
                ),
                new QAPTestClass(
                        clazz.getSimpleName(),
                        context.getDisplayName(),
                        classTags
                )
        );

        // Populate class-level metadata
        qapLaunch.getTestClass().setClassKey(clazz.getName());
        java.util.List<String> chain = new java.util.ArrayList<>();
        chain.add(context.getDisplayName());
        qapLaunch.getTestClass().setClassChain(chain);
        qapLaunch.getTestClass().setInheritedClassTags(java.util.Collections.emptySet());

        StoreManager.putClassStoreData(context, TEST_CLASS_DATA_KEY, qapLaunch);
        return qapLaunch;
    }
}
