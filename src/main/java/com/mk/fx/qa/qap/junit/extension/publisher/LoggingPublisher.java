package com.mk.fx.qa.qap.junit.extension.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;

/** Publisher that only logs the serialized payload with basic metrics and context. */
public class LoggingPublisher implements LaunchPublisher {

  @Override
  public void publish(QAPJunitLaunch launch, ObjectMapper mapper, Logger log) {
    try {
      String json = mapper.writeValueAsString(launch);
      int tests = 0;
      for (var cls : launch.getTestClasses()) {
        tests += (cls.getTestCases() != null ? cls.getTestCases().size() : 0);
      }
      int bytes = json.getBytes(StandardCharsets.UTF_8).length;
      String launchId = launch.getHeader().getLaunchId();
      String cls =
          launch.getTestClasses().isEmpty() ? "" : launch.getTestClasses().get(0).getClassName();
      log.info(
          "Publishing QAP launch: class='{}' tests={} bytes={} launchId='{}'",
          cls,
          tests,
          bytes,
          launchId);
      log.debug("QAP Launch payload: {}", json);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize QAP launch payload: {}", e.getMessage(), e);
    }
  }
}
