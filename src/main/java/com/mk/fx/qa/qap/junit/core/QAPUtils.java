package com.mk.fx.qa.qap.junit.core;

import com.mk.fx.qa.qap.junit.model.QAPHeader;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPPropertiesLoader;
import java.time.Instant;

public final class QAPUtils {

  public static final String TEST_CLASS_DATA_KEY = "testClassData";
  public static final String METHOD_DESCRIPTION_KEY = "methodDescription";
  public static final String PARAM_INDEX_KEY = "paramIndexCounter";
  public static final String CLASS_NODES_KEY = "classNodes";

  private QAPUtils() {
    // static
  }

  public static String getJunitVersion() {
    var junit = org.junit.jupiter.engine.extension.ExtensionRegistrar.class.getPackage();
    return "Junit " + junit.getImplementationVersion();
  }

  public static void buildQAPHeaders(
      QAPHeader qapHeader, String gitInfo, QAPPropertiesLoader qapAttributes) {
    qapHeader.setLaunchEndTime(Instant.now().toEpochMilli());
    qapHeader.setApplicationName(qapAttributes.getAppName());
    qapHeader.setTestEnvironment(qapAttributes.getTestEnvironment());
    // TODO: Refactor git prop
    qapHeader.setGitBranch(gitInfo);
    qapHeader.setUser(qapAttributes.getUser());
    qapHeader.setRegression(ExtensionUtil.isRegressionEnabled());
    qapHeader.setTestRunnerVersion(QAPUtils.getJunitVersion());
    qapHeader.setOsVersion(ExtensionUtil.getOsVersion());
    qapHeader.setJdkVersion(ExtensionUtil.getJdkVersion());
  }

  public static boolean isReportingEnabled(
      QAPJunitLaunch launch, QAPPropertiesLoader qapPropertiesLoader) {
    return qapPropertiesLoader.isReportingEnabled();
  }
}
