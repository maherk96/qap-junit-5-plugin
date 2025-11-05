package com.mk.fx.qa.qap.junit.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.extension.DisplayNameResolver;
import com.mk.fx.qa.qap.junit.extension.publisher.LaunchPublisher;
import com.mk.fx.qa.qap.junit.extension.publisher.StdOutPublisher;
import com.mk.fx.qa.qap.junit.model.QAPPropertiesLoader;

import java.time.Clock;
import java.util.Objects;

/**
 * Aggregates shared, injectable runtime collaborators for the JUnit extension.
 * Provides default production instances and enables test-time substitution.
 */
public class QAPRuntime {

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final QAPPropertiesLoader propertiesLoader;
    private final DisplayNameResolver displayNameResolver;
    private final LaunchPublisher launchPublisher;

    public QAPRuntime(ObjectMapper objectMapper,
                      Clock clock,
                      QAPPropertiesLoader propertiesLoader,
                      DisplayNameResolver displayNameResolver,
                      LaunchPublisher launchPublisher) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.propertiesLoader = Objects.requireNonNull(propertiesLoader, "propertiesLoader");
        this.displayNameResolver = Objects.requireNonNull(displayNameResolver, "displayNameResolver");
        this.launchPublisher = Objects.requireNonNull(launchPublisher, "launchPublisher");
    }

    public static QAPRuntime defaultRuntime() {
        return new QAPRuntime(
                new ObjectMapper(),
                Clock.systemUTC(),
                new QAPPropertiesLoader(),
                new DisplayNameResolver(),
                new StdOutPublisher()
        );
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public Clock getClock() {
        return clock;
    }

    public QAPPropertiesLoader getPropertiesLoader() {
        return propertiesLoader;
    }

    public DisplayNameResolver getDisplayNameResolver() {
        return displayNameResolver;
    }

    public LaunchPublisher getLaunchPublisher() {
        return launchPublisher;
    }
}

