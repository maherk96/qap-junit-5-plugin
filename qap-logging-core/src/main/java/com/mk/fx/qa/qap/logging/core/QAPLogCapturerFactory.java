package com.mk.fx.qa.qap.logging.core;

/**
 * Factory interface for creating QAPLogCapturer instances. Implementations are discovered at
 * runtime using Java ServiceLoader.
 *
 * <p>To register a factory implementation: Create a file:
 * META-INF/services/com.mk.fx.qa.qap.logging.core.QAPLogCapturerFactory Content:
 * fully.qualified.FactoryClassName
 *
 * <p>Example for Logback: File:
 * META-INF/services/com.mk.fx.qa.qap.logging.core.QAPLogCapturerFactory Content:
 * com.mk.fx.qa.qap.logging.logback.LogbackCapturerFactory
 */
public interface QAPLogCapturerFactory {

  /**
   * Creates a new instance of a log capturer. Called by the registry during discovery.
   *
   * @return a new QAPLogCapturer instance
   */
  QAPLogCapturer create();

  /**
   * Returns the name of this factory for diagnostic purposes. Should match the framework name
   * (e.g., "Logback", "Log4j2").
   *
   * @return factory name
   */
  default String getName() {
    return create().getFrameworkName();
  }
}
