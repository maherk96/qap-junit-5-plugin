package com.mk.fx.qa.qap.junit.extension;

import org.junit.jupiter.api.extension.ExtensionContext;

public interface ILifeCycleEventCreator {

    void createLifeCycleEvent(LifeCycleEvent event, ExtensionContext context);
}