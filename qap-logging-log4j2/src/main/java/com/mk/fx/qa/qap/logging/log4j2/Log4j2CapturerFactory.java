package com.mk.fx.qa.qap.logging.log4j2;

import com.mk.fx.qa.qap.logging.core.QAPLogCapturer;
import com.mk.fx.qa.qap.logging.core.QAPLogCapturerFactory;

/**
 * Factory for creating Log4j2Capturer instances. Discovered at runtime by Java ServiceLoader.
 *
 * <p>This factory is registered in META-INF/services/QAPLogCapturerFactory.
 */
public class Log4j2CapturerFactory implements QAPLogCapturerFactory {

  @Override
  public QAPLogCapturer create() {
    return new Log4j2Capturer();
  }

  @Override
  public String getName() {
    return "Log4j2";
  }
}
