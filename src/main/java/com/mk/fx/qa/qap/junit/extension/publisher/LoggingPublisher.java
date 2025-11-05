package com.mk.fx.qa.qap.junit.extension.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;

/**
 * Publisher that only logs the serialized payload with basic metrics and context.
 */
public class LoggingPublisher implements LaunchPublisher {

    @Override
    public void publish(QAPJunitLaunch launch, ObjectMapper mapper, Logger log) {
        try {
            String json = mapper.writeValueAsString(launch);
            int tests = launch.getTestClass().getTestCases() != null ? launch.getTestClass().getTestCases().size() : 0;
            int bytes = json.getBytes(StandardCharsets.UTF_8).length;
            String launchId = launch.getHeader().getLaunchId();
            String cls = launch.getTestClass().getClassName();
            log.info("Publishing QAP launch: class='{}' tests={} bytes={} launchId='{}'", cls, tests, bytes, launchId);
            log.debug("QAP Launch payload: {}", json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize QAP launch payload: {}", e.getMessage(), e);
        }
    }
}

