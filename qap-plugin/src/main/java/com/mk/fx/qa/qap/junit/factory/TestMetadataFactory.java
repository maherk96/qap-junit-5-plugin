package com.mk.fx.qa.qap.junit.factory;

import com.mk.fx.qa.qap.junit.extension.DisplayNameResolver;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import java.util.Objects;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Factory for creating QAPTest metadata objects from JUnit extension contexts. Handles display name
 * resolution and test identification.
 */
public final class TestMetadataFactory {

  private TestMetadataFactory() {}

  /**
   * Creates a QAPTest metadata object from the extension context.
   *
   * @param context the JUnit extension context (must not be null)
   * @param resolver the display name resolver (must not be null)
   * @return a new QAPTest instance with metadata populated
   * @throws NullPointerException if context or resolver is null
   */
  public static QAPTest create(ExtensionContext context, DisplayNameResolver resolver) {
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(resolver, "resolver cannot be null");

    String methodName = context.getRequiredTestMethod().getName();
    String rawDisplay = context.getDisplayName();

    String runDisplay = resolver.resolveRunDisplayName(context, methodName, rawDisplay);
    QAPTest test = new QAPTest(methodName, runDisplay);
    test.setMethodDisplayName(resolver.resolveMethodDisplayName(context));

    return test;
  }
}
