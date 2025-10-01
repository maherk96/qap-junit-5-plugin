package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.core.TestCaseStatus;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import org.junit.jupiter.api.extension.ExtensionContext;

public interface ITestEventCreator {

    void addTestEventsToTestLaunch(ExtensionContext context, QAPJunitLaunch launch);

    void createTestTemplate(ExtensionContext context, TestCaseStatus status, Throwable t);

    QAPJunitLaunch startLaunchQAP(ExtensionContext context);
}