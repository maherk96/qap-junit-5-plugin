package com.mk.fx.qa.qap.junit.extension.support;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Simple in-memory implementation of JUnit's ExtensionContext.Store
 * for unit testing without relying on real JUnit stores.
 */
public class InMemoryStore implements ExtensionContext.Store {
    private final Map<Object, Object> map = new HashMap<>();

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V get(Object key, Class<V> requiredType) {
        Object v = map.get(key);
        if (v == null) return null;
        return (V) v;
    }

    @Override
    public void put(Object key, Object value) {
        map.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return map.remove(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V remove(Object key, Class<V> requiredType) {
        Object v = map.remove(key);
        return (V) v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> V getOrComputeIfAbsent(K key, Function<K, V> defaultCreator) {
        Object existing = map.get(key);
        if (existing != null) return (V) existing;
        V created = defaultCreator.apply(key);
        map.put(key, created);
        return created;
    }

    @SuppressWarnings("unchecked")
    public <K, V> V getOrComputeIfAbsent(K key, Class<V> requiredType, Function<K, V> defaultCreator) {
        Object existing = map.get(key);
        if (existing != null) return (V) existing;
        V created = defaultCreator.apply(key);
        map.put(key, created);
        return created;
    }

    // Some JUnit versions declare this overload with parameters in a different order
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> V getOrComputeIfAbsent(K key, Function<K, V> defaultCreator, Class<V> requiredType) {
        Object existing = map.get(key);
        if (existing != null) return (V) existing;
        V created = defaultCreator.apply(key);
        map.put(key, created);
        return created;
    }

    @SuppressWarnings("unchecked")
    public <V> V getOrDefault(Object key, Class<V> requiredType, V defaultValue) {
        Object existing = map.get(key);
        if (existing == null) return defaultValue;
        return (V) existing;
    }
}
