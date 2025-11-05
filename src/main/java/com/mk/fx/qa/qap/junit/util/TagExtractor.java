package com.mk.fx.qa.qap.junit.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class TagExtractor {

    private TagExtractor() {}

    /**
     * Extracts only the tags declared directly on the test method.
     */
    public static Set<String> methodTags(ExtensionContext context) {
        return context.getTestMethod()
                .map(m -> {
                    Set<String> tags = new HashSet<>();
                    for (Tag t : m.getAnnotationsByType(Tag.class)) {
                        tags.add(t.value());
                    }
                    return Collections.unmodifiableSet(tags);
                })
                .orElse(Collections.emptySet());
    }

    /**
     * Collects @Tag annotations declared on the current test class only.
     */
    public static Set<String> classTags(ExtensionContext context) {
        return context.getTestClass()
                .map(c -> {
                    Set<String> tags = new HashSet<>();
                    for (Tag t : c.getAnnotationsByType(Tag.class)) {
                        tags.add(t.value());
                    }
                    return Collections.unmodifiableSet(tags);
                })
                .orElse(Collections.emptySet());
    }

    /**
     * Walks up the parent chain and collects @Tag annotations from enclosing classes only
     * (excluding the current test class).
     */
    public static Set<String> inheritedClassTags(ExtensionContext context) {
        Set<String> tags = new HashSet<>();
        Optional<Class<?>> current = context.getTestClass();
        Optional<ExtensionContext> parent = context.getParent();
        while (parent.isPresent()) {
            ExtensionContext p = parent.get();
            Optional<Class<?>> cls = p.getTestClass();
            if (cls.isPresent() && (current.isEmpty() || !cls.get().equals(current.get()))) {
                for (Tag t : cls.get().getAnnotationsByType(Tag.class)) {
                    tags.add(t.value());
                }
            }
            parent = p.getParent();
        }
        return Collections.unmodifiableSet(tags);
    }
}
