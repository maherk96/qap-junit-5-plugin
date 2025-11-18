package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.core.QAPUtils;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies that the extension aggregates nested test results under the top-level class
 * and only serializes once at the top-level afterAll.
 */
class QAPJunitExtensionAggregationTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outBuffer;

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // A simple top-level class with a nested class to simulate a hierarchy
    static class TopLevelClass {
        @DisplayName("NestedGroup")
        static class NestedClass {
            void sampleTestMethod() {}
        }
    }

    @Test
    void aggregatesNestedAndEmitsOnceAtTopLevel() throws Exception {
        // Capture System.out prints from the extension's serialization
        outBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer));

        // Shared root store so all contexts see the same storage
        ExtensionContext root = Mockito.mock(ExtensionContext.class);
        Map<ExtensionContext.Namespace, ExtensionContext.Store> stores = new HashMap<>();
        when(root.getStore(any())).thenAnswer(inv -> stores.computeIfAbsent(
                inv.getArgument(0), ns -> new MapBackedStore()));
        when(root.getParent()).thenReturn(Optional.empty());
        when(root.getRoot()).thenReturn(root);

        // Top-level class context
        ExtensionContext top = Mockito.mock(ExtensionContext.class);
        when(top.getRoot()).thenReturn(root);
        when(top.getParent()).thenReturn(Optional.of(root));
        when(top.getTestClass()).thenReturn(Optional.of(TopLevelClass.class));
        when(top.getRequiredTestClass()).thenAnswer(inv -> TopLevelClass.class);
        when(top.getDisplayName()).thenReturn("TopLevelClass");

        // Nested class context
        ExtensionContext nested = Mockito.mock(ExtensionContext.class);
        when(nested.getRoot()).thenReturn(root);
        when(nested.getParent()).thenReturn(Optional.of(top));
        when(nested.getTestClass()).thenReturn(Optional.of(TopLevelClass.NestedClass.class));
        when(nested.getRequiredTestClass()).thenAnswer(inv -> TopLevelClass.NestedClass.class);
        when(nested.getDisplayName()).thenReturn("NestedGroup");

        // Method context under the nested class
        Method m = TopLevelClass.NestedClass.class.getDeclaredMethod("sampleTestMethod");
        ExtensionContext methodCtx = Mockito.mock(ExtensionContext.class);
        when(methodCtx.getRoot()).thenReturn(root);
        when(methodCtx.getParent()).thenReturn(Optional.of(nested));
        when(methodCtx.getTestClass()).thenReturn(Optional.of(TopLevelClass.NestedClass.class));
        when(methodCtx.getRequiredTestClass()).thenAnswer(inv -> TopLevelClass.NestedClass.class);
        when(methodCtx.getDisplayName()).thenReturn("sampleTestMethod()");
        when(methodCtx.getTestMethod()).thenReturn(Optional.of(m));
        when(methodCtx.getRequiredTestMethod()).thenReturn(m);

        // Exercise the extension lifecycle
        QAPJunitExtension ext = new QAPJunitExtension();
        ext.beforeAll(top);      // sets up launch
        ext.beforeAll(nested);   // nested beforeAll should not create/emit a new launch

        // Simulate one test run under the nested class
        ext.beforeEach(methodCtx);
        ext.testSuccessful(methodCtx);
        ext.afterEach(methodCtx);

        // Nested afterAll: should NOT serialize
        ext.afterAll(nested);
        String mid = outBuffer.toString().trim();
        assertTrue(mid.isEmpty(), "Nested afterAll must not emit JSON");

        // Top-level afterAll: should serialize once with aggregated test(s)
        ext.afterAll(top);
        String all = outBuffer.toString();
        assertFalse(all.isEmpty(), "Top-level afterAll should emit JSON");

        // Basic shape assertions
        assertTrue(all.contains("\"testClasses\""), "JSON should include testClasses array");
        assertTrue(all.contains("TopLevelClass"), "Should reference top-level class name");
        assertTrue(all.contains("\"testCases\""), "Should include aggregated test cases");

        // Lifecycle events are collected for both top and nested contexts
        QAPJunitLaunch launch = StoreManager.getClassStoreData(top, QAPUtils.TEST_CLASS_DATA_KEY, QAPJunitLaunch.class);
        assertNotNull(launch);
    }

    @Test
    void storeManagerResolvesTopLevelFromNestedChain() {
        // Build a chain: method -> nested -> top -> root
        ExtensionContext root = Mockito.mock(ExtensionContext.class);
        when(root.getParent()).thenReturn(Optional.empty());

        ExtensionContext top = Mockito.mock(ExtensionContext.class);
        when(top.getTestClass()).thenReturn(Optional.of(TopLevelClass.class));
        when(top.getParent()).thenReturn(Optional.of(root));

        ExtensionContext nested = Mockito.mock(ExtensionContext.class);
        when(nested.getTestClass()).thenReturn(Optional.of(TopLevelClass.NestedClass.class));
        when(nested.getParent()).thenReturn(Optional.of(top));

        Class<?> resolved = StoreManager.resolveTopLevelTestClass(nested);
        assertEquals(TopLevelClass.class, resolved, "Should resolve the top-most test class");
    }

    /** Simple in-memory Store for tests. */
    static class MapBackedStore implements ExtensionContext.Store {
        private final Map<Object, Object> data = new HashMap<>();

        @Override
        public Object get(Object key) {
            return data.get(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> V get(Object key, Class<V> requiredType) {
            return (V) data.get(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> V getOrDefault(Object key, Class<V> requiredType, V defaultValue) {
            return (V) data.getOrDefault(key, defaultValue);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> V getOrComputeIfAbsent(K key, java.util.function.Function<K, V> defaultCreator, Class<V> requiredType) {
            Object value = data.get(key);
            if (value == null) {
                V created = defaultCreator.apply(key);
                data.put(key, created);
                return created;
            }
            return (V) value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> V getOrComputeIfAbsent(K key, java.util.function.Function<K, V> defaultCreator) {
            Object value = data.get(key);
            if (value == null) {
                V created = defaultCreator.apply(key);
                data.put(key, created);
                return created;
            }
            return (V) value;
        }

        @Override
        public void put(Object key, Object value) {
            data.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return data.remove(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> V remove(Object key, Class<V> valueType) {
            return (V) data.remove(key);
        }
    }
}
