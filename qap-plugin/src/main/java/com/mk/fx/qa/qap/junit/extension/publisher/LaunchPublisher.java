package com.mk.fx.qa.qap.junit.extension.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import org.slf4j.Logger;

/** Strategy interface for publishing a launch payload (e.g., to logs, stdout, file, or HTTP). */
public interface LaunchPublisher {

  void publish(QAPJunitLaunch launch, ObjectMapper mapper, Logger log);
}
