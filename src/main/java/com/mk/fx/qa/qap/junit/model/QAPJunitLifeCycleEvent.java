package com.mk.fx.qa.qap.junit.model;

import com.mk.fx.qa.qap.junit.extension.LifeCycleEvent;

public record QAPJunitLifeCycleEvent(LifeCycleEvent event, byte[] exception, byte[] logs) { }