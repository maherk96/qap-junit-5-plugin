package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.extension.support.InMemoryStore;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QAPJunitMethodInterceptorTest {

    static class Dummy {
        void m(String a, Integer b) {}
    }

    @Test
    void interceptor_sets_parameters_list_with_arguments() throws Throwable {
        // Arrange extension context + store
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        ExtensionContext root = mock(ExtensionContext.class);
        when(ctx.getRoot()).thenReturn(root);
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        Method m = Dummy.class.getDeclaredMethod("m", String.class, Integer.class);
        when(ctx.getRequiredTestMethod()).thenReturn(m);
        when(ctx.getRequiredTestClass()).thenReturn((Class) Dummy.class);

        // Seed a QAPTest in method store
        QAPTest test = new QAPTest("m", "m");
        store.put(METHOD_DESCRIPTION_KEY, test);

        // Mocks for invocation context
        @SuppressWarnings("unchecked")
        ReflectiveInvocationContext<Method> ric = mock(ReflectiveInvocationContext.class);
        when(ric.getArguments()).thenReturn(List.of("A", 42));
        InvocationInterceptor.Invocation<Void> invocation = mock(InvocationInterceptor.Invocation.class);

        QAPJunitMethodInterceptor interceptor = new QAPJunitMethodInterceptor(new ConcurrentHashMap<>());

        // Act
        interceptor.interceptTestTemplateMethod(invocation, ric, ctx);

        // Assert
        QAPTest stored = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertNotNull(stored);
        assertTrue(stored.hasParameters());
        assertEquals(2, stored.getParameters().size());
        assertEquals(0, stored.getParameters().get(0).argumentIndex());
        assertEquals("String", stored.getParameters().get(0).argumentType());
        assertEquals("A", stored.getParameters().get(0).argumentValue());
        assertEquals(1, stored.getParameters().get(1).argumentIndex());
        assertEquals("Integer", stored.getParameters().get(1).argumentType());
        assertEquals("Dummy#m[0]", stored.getTestCaseId());
        verify(invocation).proceed();
    }
}
