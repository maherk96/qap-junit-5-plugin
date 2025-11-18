package com.mk.fx.qa.qap.junit.factory;

import com.mk.fx.qa.qap.junit.extension.DisplayNameResolver;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class TestMetadataFactory {

  private TestMetadataFactory() {}

  public static QAPTest create(ExtensionContext context, DisplayNameResolver resolver) {
    String methodName = context.getRequiredTestMethod().getName();
    String rawDisplay = context.getDisplayName();

    String runDisplay = resolver.resolveRunDisplayName(context, methodName, rawDisplay);
    QAPTest test = new QAPTest(methodName, runDisplay);
    test.setMethodDisplayName(resolver.resolveMethodDisplayName(context));

    return test;
  }
}
