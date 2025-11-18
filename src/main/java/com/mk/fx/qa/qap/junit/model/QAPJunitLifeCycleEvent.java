package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mk.fx.qa.qap.junit.extension.LifeCycleEvent;

import java.util.Collections;
import java.util.List;

public class QAPJunitLifeCycleEvent {

    private final LifeCycleEvent event;
    private final byte[] exception;

    @JsonIgnore
    private final byte[] logs;

    public QAPJunitLifeCycleEvent(LifeCycleEvent event, byte[] exception, byte[] logs) {
        this.event = event;
        this.exception = exception;
        this.logs = logs;
    }

    public LifeCycleEvent getEvent() {
        return event;
    }

    public byte[] getException() {
        return exception;
    }

    // Always serialize logs as an array for lifecycle events
    @JsonProperty("logs")
    public List<String> getLogsArray() {
        return Collections.emptyList();
    }
}
