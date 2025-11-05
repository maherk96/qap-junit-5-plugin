package com.mk.fx.qa.qap.junit.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class QAPLaunchIdGeneratorConcurrencyTest {

    @AfterEach
    void clear() {
        System.clearProperty("launchID");
    }

    @Test
    void generateIfAbsent_is_idempotent_under_concurrency() throws Exception {
        System.clearProperty("launchID");
        QAPLaunchIdGenerator gen = new QAPLaunchIdGenerator();

        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    gen.generateIfAbsent();
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
            list.add(t);
            t.start();
        }
        start.countDown();
        done.await();

        String first = gen.getLaunchId();
        assertNotNull(first);
        assertTrue(first.matches(".+[-a-zA-Z0-9]{12,}"));

        gen.generateIfAbsent();
        String second = gen.getLaunchId();
        assertEquals(first, second, "generateIfAbsent must not change existing id");
    }
}

