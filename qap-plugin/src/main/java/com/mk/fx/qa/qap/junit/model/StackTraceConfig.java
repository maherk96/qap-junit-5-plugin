package com.mk.fx.qa.qap.junit.model;

import lombok.Builder;
import lombok.Value;

/** Configuration for stack trace capture and formatting. */
@Value
@Builder
public class StackTraceConfig {

  /** Maximum number of stack trace lines to include. -1 means unlimited. */
  int maxLines;

  /**
   * Number of lines to keep from the start of the stack trace (first frames showing the error
   * origin).
   */
  int headLines;

  /**
   * Number of lines to keep from the end of the stack trace (last frames showing test runner
   * entry).
   */
  int tailLines;

  /**
   * If true, keeps all frames until we exit user-code (i.e., stop when we hit framework code
   * again).
   */
  boolean keepUntilFrameworkExit;

  /**
   * Creates default configuration: max 200 lines, keep first 50 + last 20, don't stop at framework
   * exit.
   */
  public static StackTraceConfig defaultConfig() {
    return StackTraceConfig.builder()
        .maxLines(200)
        .headLines(50)
        .tailLines(20)
        .keepUntilFrameworkExit(false)
        .build();
  }

  /**
   * Creates configuration from properties loader using pre-loaded values.
   *
   * @param loader properties loader with qap.properties values
   * @return StackTraceConfig based on properties or defaults
   */
  public static StackTraceConfig fromProperties(QAPPropertiesLoader loader) {
    if (loader == null) {
      return defaultConfig();
    }

    return StackTraceConfig.builder()
        .maxLines(loader.getStackTraceMaxLines())
        .headLines(loader.getStackTraceHeadLines())
        .tailLines(loader.getStackTraceTailLines())
        .keepUntilFrameworkExit(loader.isStackTraceKeepUntilFrameworkExit())
        .build();
  }
}
