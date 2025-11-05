package com.mk.fx.qa.qap.junit.factory;

import com.mk.fx.qa.qap.junit.extension.DisplayNameResolver;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TestMetadataFactoryTest {

    @DisplayName("Outer Context")
    static class Outer {
        @DisplayName("Inner Context")
        static class Inner {
            @DisplayName("Custom Method")
            void sample() {}
        }
    }

    @Test
    void factory_populates_display_and_parent_fields() throws Exception {
        DisplayNameResolver resolver = new DisplayNameResolver();

        Method m = Outer.Inner.class.getDeclaredMethod("sample");

        ExtensionContext root = Mockito.mock(ExtensionContext.class);
        when(root.getParent()).thenReturn(Optional.empty());

        ExtensionContext outer = Mockito.mock(ExtensionContext.class);
        when(outer.getParent()).thenReturn(Optional.of(root));
        when(outer.getTestClass()).thenReturn(Optional.of(Outer.class));

        ExtensionContext inner = Mockito.mock(ExtensionContext.class);
        when(inner.getParent()).thenReturn(Optional.of(outer));
        when(inner.getTestClass()).thenReturn(Optional.of(Outer.Inner.class));

        ExtensionContext methodCtx = Mockito.mock(ExtensionContext.class);
        when(methodCtx.getParent()).thenReturn(Optional.of(inner));
        when(methodCtx.getTestClass()).thenReturn(Optional.of(Outer.Inner.class));
        when(methodCtx.getDisplayName()).thenReturn("sample()");
        when(methodCtx.getTestMethod()).thenReturn(Optional.of(m));
        when(methodCtx.getRequiredTestMethod()).thenReturn(m);

        QAPTest test = TestMetadataFactory.create(methodCtx, resolver);

        assertEquals("sample", test.getMethodName());
        assertEquals("Custom Method", test.getMethodDisplayName());
        assertEquals("Inner Context", test.getParentDisplayName());
        assertTrue(test.getParentClassKey().endsWith("Outer$Inner"));

        // Parent chain includes Outer then Inner (current) appended by factory
        assertEquals(2, test.getParentChain().size());
        assertEquals("Outer Context", test.getParentChain().get(0));
        assertEquals("Inner Context", test.getParentChain().get(1));
    }
}

