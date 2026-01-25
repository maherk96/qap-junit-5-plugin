package com.mk.fx.qa.qap.logging.logback;

import com.mk.fx.qa.qap.logging.core.QAPLogCapturer;
import com.mk.fx.qa.qap.logging.core.QAPLogCapturerFactory;

/**
 * Factory for creating LogbackCapturer instances. Discovered at runtime by Java ServiceLoader.
 *
 * <p>This factory is registered in META-INF/services/QAPLogCapturerFactory.
 */
public class LogbackCapturerFactory implements QAPLogCapturerFactory {

  @Override
  public QAPLogCapturer create() {
    return new LogbackCapturer();
  }

  @Override
  public String getName() {
    return "Logback";
  }
}
