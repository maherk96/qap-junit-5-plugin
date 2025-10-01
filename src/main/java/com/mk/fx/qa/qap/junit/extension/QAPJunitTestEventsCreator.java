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

import static com.mk.fx.qa.qap.junit.core.QAPUtils.TEST_CLASS_DATA_KEY;


public class QAPJunitTestEventsCreator implements ITestEventCreator {

    private static final String SYSTEM_PROPERTY_LAUNCH_ID = "launchID";

    @Override
    public void addTestEventsToTestLaunch(ExtensionContext context, QAPJunitLaunch launch) {
        var events = StoreManager.getClassStoreData(
                context, QAPUtils.METHOD_DESCRIPTION_KEY, ArrayList.class);
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
        var qapLaunch = new QAPJunitLaunch(
                new QAPHeader(
                        Instant.now().toEpochMilli(),
                        System.getProperty(SYSTEM_PROPERTY_LAUNCH_ID)
                ),
                new QAPTestClass(
                        context.getRequiredTestClass().getSimpleName(),
                        context.getDisplayName(),
                        context.getTags()
                )
        );

        StoreManager.putClassStoreData(context, TEST_CLASS_DATA_KEY, qapLaunch);
        return qapLaunch;
    }
}