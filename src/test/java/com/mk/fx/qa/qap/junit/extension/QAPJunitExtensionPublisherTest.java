package com.mk.fx.qa.qap.junit.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.fx.qa.qap.junit.extension.publisher.LaunchPublisher;
import com.mk.fx.qa.qap.junit.model.QAPPropertiesLoader;
import com.mk.fx.qa.qap.junit.runtime.QAPRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests publisher invocation paths: recovery when launch is missing and single publish for nested.
 */
class QAPJunitExtensionPublisherTest {

    static class CountingPublisher implements LaunchPublisher {
        volatile int count = 0;
        @Override
        public void publish(com.mk.fx.qa.qap.junit.model.QAPJunitLaunch launch, ObjectMapper mapper, org.slf4j.Logger log) {
            count++;
        }
    }

    static class MapBackedStore implements ExtensionContext.Store {
        private final Map<Object, Object> data = new HashMap<>();
        @Override public Object get(Object key) { return data.get(key); }
        @Override public <V> V get(Object key, Class<V> requiredType) { return requiredType.cast(data.get(key)); }
        @Override public <V> V getOrDefault(Object key, Class<V> requiredType, V defaultValue) { return requiredType.cast(data.getOrDefault(key, defaultValue)); }
        @Override public <K, V> V getOrComputeIfAbsent(K key, java.util.function.Function<K, V> defaultCreator, Class<V> requiredType) {
            return requiredType.cast(data.computeIfAbsent(key, k -> defaultCreator.apply(key)));
        }
        @Override public <K, V> V getOrComputeIfAbsent(K key, java.util.function.Function<K, V> defaultCreator) {
            @SuppressWarnings("unchecked") V v = (V) data.computeIfAbsent(key, k -> defaultCreator.apply(key));
            return v;
        }
        @Override public void put(Object key, Object value) { data.put(key, value); }
        @Override public Object remove(Object key) { return data.remove(key); }
        @Override public <V> V remove(Object key, Class<V> valueType) { return valueType.cast(data.remove(key)); }
    }

    private ExtensionContext root(Map<ExtensionContext.Namespace, ExtensionContext.Store> stores) {
        ExtensionContext root = Mockito.mock(ExtensionContext.class);
        when(root.getStore(any())).thenAnswer(inv -> stores.computeIfAbsent(inv.getArgument(0), ns -> new MapBackedStore()));
        when(root.getParent()).thenReturn(Optional.empty());
        when(root.getRoot()).thenReturn(root);
        return root;
    }

    @Test
    void afterAll_missing_launch_recovers_and_publishes_once() throws Exception {
        CountingPublisher publisher = new CountingPublisher();
        QAPRuntime runtime = new QAPRuntime(new ObjectMapper(), Clock.systemUTC(), new QAPPropertiesLoader(), new DisplayNameResolver(), publisher);
        QAPJunitExtension ext = new QAPJunitExtension(
                runtime,
                new QAPJunitLifeCycleEventCreator(new java.util.concurrent.ConcurrentHashMap<>()),
                new QAPJunitTestEventsCreator(),
                new QAPJunitMethodInterceptor(new java.util.concurrent.ConcurrentHashMap<>()),
                new com.mk.fx.qa.qap.junit.core.QAPLaunchIdGenerator()
        );

        Map<ExtensionContext.Namespace, ExtensionContext.Store> stores = new HashMap<>();
        ExtensionContext root = root(stores);

        class Top { void m() {} }
        ExtensionContext top = Mockito.mock(ExtensionContext.class);
        when(top.getRoot()).thenReturn(root);
        when(top.getParent()).thenReturn(Optional.of(root));
        when(top.getTestClass()).thenReturn(Optional.of(Top.class));
        when(top.getRequiredTestClass()).thenAnswer(__ -> Top.class);
        when(top.getDisplayName()).thenReturn("Top");

        // Simulate one test to produce testCases for reporting
        java.lang.reflect.Method method = Top.class.getDeclaredMethod("m");
        ExtensionContext methodCtx = Mockito.mock(ExtensionContext.class);
        when(methodCtx.getRoot()).thenReturn(root);
        when(methodCtx.getParent()).thenReturn(Optional.of(top));
        when(methodCtx.getTestClass()).thenReturn(Optional.of(Top.class));
        when(methodCtx.getRequiredTestClass()).thenAnswer(__ -> Top.class);
        when(methodCtx.getDisplayName()).thenReturn("m()");
        when(methodCtx.getTestMethod()).thenReturn(Optional.of(method));
        when(methodCtx.getRequiredTestMethod()).thenReturn(method);

        // No beforeAll(top) => missing launch, but tests still recorded
        ext.beforeEach(methodCtx);
        ext.testSuccessful(methodCtx);
        ext.afterEach(methodCtx);
        ext.afterAll(top);
        assertEquals(1, publisher.count, "afterAll should recover and publish once");
    }

    @Test
    void nested_afterAll_does_not_publish_top_level_does_once() throws Exception {
        CountingPublisher publisher = new CountingPublisher();
        QAPRuntime runtime = new QAPRuntime(new ObjectMapper(), Clock.systemUTC(), new QAPPropertiesLoader(), new DisplayNameResolver(), publisher);
        QAPJunitExtension ext = new QAPJunitExtension(
                runtime,
                new QAPJunitLifeCycleEventCreator(new java.util.concurrent.ConcurrentHashMap<>()),
                new QAPJunitTestEventsCreator(),
                new QAPJunitMethodInterceptor(new java.util.concurrent.ConcurrentHashMap<>()),
                new com.mk.fx.qa.qap.junit.core.QAPLaunchIdGenerator()
        );

        Map<ExtensionContext.Namespace, ExtensionContext.Store> stores = new HashMap<>();
        ExtensionContext root = root(stores);

        class Top { static class Nested { void m() {} } }
        ExtensionContext top = Mockito.mock(ExtensionContext.class);
        when(top.getRoot()).thenReturn(root);
        when(top.getParent()).thenReturn(Optional.of(root));
        when(top.getTestClass()).thenReturn(Optional.of(Top.class));
        when(top.getRequiredTestClass()).thenAnswer(__ -> Top.class);
        when(top.getDisplayName()).thenReturn("Top");

        ExtensionContext nested = Mockito.mock(ExtensionContext.class);
        when(nested.getRoot()).thenReturn(root);
        when(nested.getParent()).thenReturn(Optional.of(top));
        when(nested.getTestClass()).thenReturn(Optional.of(Top.Nested.class));
        when(nested.getRequiredTestClass()).thenAnswer(__ -> Top.Nested.class);
        when(nested.getDisplayName()).thenReturn("Nested");

        ext.beforeAll(top);
        ext.beforeAll(nested);

        // Simulate one test in nested class to produce testCases
        java.lang.reflect.Method method = Top.Nested.class.getDeclaredMethod("m");
        ExtensionContext methodCtx = Mockito.mock(ExtensionContext.class);
        when(methodCtx.getRoot()).thenReturn(root);
        when(methodCtx.getParent()).thenReturn(Optional.of(nested));
        when(methodCtx.getTestClass()).thenReturn(Optional.of(Top.Nested.class));
        when(methodCtx.getRequiredTestClass()).thenAnswer(__ -> Top.Nested.class);
        when(methodCtx.getDisplayName()).thenReturn("m()");
        when(methodCtx.getTestMethod()).thenReturn(Optional.of(method));
        when(methodCtx.getRequiredTestMethod()).thenReturn(method);

        ext.beforeEach(methodCtx);
        ext.testSuccessful(methodCtx);
        ext.afterEach(methodCtx);

        ext.afterAll(nested);
        assertEquals(0, publisher.count, "nested afterAll must not publish");

        ext.afterAll(top);
        assertEquals(1, publisher.count, "top-level afterAll should publish once");
    }
}
