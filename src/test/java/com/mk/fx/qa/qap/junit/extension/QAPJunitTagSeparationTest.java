package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.extension.support.InMemoryStore;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QAPJunitTagSeparationTest {

    @Tag("ClassTag")
    static class AnnotatedTestClass {
        @Tag("MethodTag")
        @DisplayName("Method DN")
        void someTest() {}
    }

    @Test
    void classTags_populated_from_class_annotations_only() throws Exception {
        // Arrange
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getRequiredTestClass()).thenReturn((Class) AnnotatedTestClass.class);
        when(ctx.getDisplayName()).thenReturn("AnnotatedTestClass");

        QAPJunitTestEventsCreator creator = new QAPJunitTestEventsCreator();

        // Act
        var launch = creator.startLaunchQAP(ctx);

        // Assert
        assertEquals("AnnotatedTestClass", launch.getTestClass().getClassName());
        assertEquals(Set.of("ClassTag"), launch.getTestClass().getClassTags());
    }

    @Test
    void methodTags_populated_from_method_annotations_only() throws Exception {
        // Arrange context and in-memory store
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        ExtensionContext root = mock(ExtensionContext.class);
        when(ctx.getRoot()).thenReturn(root);
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        when(ctx.getRequiredTestClass()).thenReturn((Class) AnnotatedTestClass.class);
        when(ctx.getTestClass()).thenReturn(Optional.of(AnnotatedTestClass.class));

        Method m = AnnotatedTestClass.class.getDeclaredMethod("someTest");
        when(ctx.getRequiredTestMethod()).thenReturn(m);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));
        when(ctx.getDisplayName()).thenReturn("someTest()");

        // Also set combined tags in context (should NOT be used for method tags)
        when(ctx.getTags()).thenReturn(Set.of("ClassTag", "MethodTag"));

        QAPJunitExtension ext = new QAPJunitExtension();

        // Act
        ext.beforeEach(ctx);

        // Assert
        QAPTest q = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertNotNull(q);
        assertEquals(Set.of("MethodTag"), q.getTags().getMethod());
        assertEquals("Method DN", q.getMethodDisplayName());
    }
}
