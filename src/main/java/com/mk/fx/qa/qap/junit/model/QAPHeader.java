package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class QAPHeader {

    private final long launchStartTime;
    private String applicationName;
    private String testEnvironment;
    private String user;
    private final String launchId;
    private String gitBranch;
    private boolean isRegression;
    private long launchEndTime;
    private String osVersion;
    private String testRunnerVersion;
    private String jdkVersion;

    @JsonCreator
    public QAPHeader(
            @JsonProperty("launchStartTime") long launchStartTime,
            @JsonProperty("launchId") String launchId) {
        this.launchStartTime = launchStartTime;
        this.launchId = launchId;
    }
}
