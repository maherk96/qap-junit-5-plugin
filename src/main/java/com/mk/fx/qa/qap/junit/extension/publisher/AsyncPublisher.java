package com.mk.fx.qa.qap.junit.extension.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Decorator that delegates publishing to a background single-threaded executor.
 */
public class AsyncPublisher implements LaunchPublisher {
    private final LaunchPublisher delegate;
    private final ExecutorService executor;

    public AsyncPublisher(LaunchPublisher delegate) {
        this(delegate, Executors.newSingleThreadExecutor(new NamedThreadFactory("qap-publisher")));
    }

    public AsyncPublisher(LaunchPublisher delegate, ExecutorService executor) {
        this.delegate = Objects.requireNonNull(delegate);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public void publish(QAPJunitLaunch launch, ObjectMapper mapper, Logger log) {
        executor.submit(() -> delegate.publish(launch, mapper, log));
    }

    static class NamedThreadFactory implements ThreadFactory {
        private final String baseName;
        private int idx = 0;

        NamedThreadFactory(String baseName) { this.baseName = baseName; }

        @Override
        public synchronized Thread newThread(Runnable r) {
            Thread t = new Thread(r, baseName + "-" + (++idx));
            t.setDaemon(true);
            return t;
        }
    }
}

