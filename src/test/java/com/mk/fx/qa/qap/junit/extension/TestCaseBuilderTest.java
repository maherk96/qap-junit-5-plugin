package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.extension.support.InMemoryStore;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Test Case Builder Unit Tests")
class TestCaseBuilderTest {

    // ---- Fixtures ----------------------------------------------------------

    @Tag("ParentTag")
    static class Parent {}

    @Tag("ClassTag")
    static class Child {
        @Tag("MethodTag")
        @DisplayName("My Display Name")
        void myTest() {}

        void noDisplay() {}
    }

    static class MyClass { void myTest() {} }

    static class Outer { static class Inner { void myTest() {} } }

    static class ParamHolder { void myTest(String a, int b, boolean c) {} }

    // ---- Tests -------------------------------------------------------------

    @Test
    @DisplayName("Should build test case with all fields")
    void shouldBuildCompleteTestCase() throws Exception {
        // Arrange context chain: parent -> child -> method
        ExtensionContext root = mock(ExtensionContext.class);
        when(root.getParent()).thenReturn(Optional.empty());

        ExtensionContext parent = mock(ExtensionContext.class);
        when(parent.getTestClass()).thenReturn(Optional.of(Parent.class));
        when(parent.getParent()).thenReturn(Optional.of(root));
        when(parent.getRoot()).thenReturn(root);

        Method m = Child.class.getDeclaredMethod("myTest");
        ExtensionContext methodCtx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        when(methodCtx.getParent()).thenReturn(Optional.of(parent));
        when(methodCtx.getRoot()).thenReturn(root);
        when(methodCtx.getTestClass()).thenReturn(Optional.of(Child.class));
        when(methodCtx.getRequiredTestClass()).thenReturn((Class) Child.class);
        when(methodCtx.getTestMethod()).thenReturn(Optional.of(m));
        when(methodCtx.getRequiredTestMethod()).thenReturn(m);
        when(methodCtx.getDisplayName()).thenReturn("myTest()"); // auto-generated -> resolver uses @DisplayName

        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        QAPJunitExtension ext = new QAPJunitExtension();

        // Act
        ext.beforeEach(methodCtx);           // builds QAPTest and stores it
        ext.testSuccessful(methodCtx);       // sets status, end time

        // Assert
        QAPTest t = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertNotNull(t);
        assertEquals("myTest", t.getMethodName());
        assertEquals("My Display Name", t.getDisplayName(), "run display name prefers @DisplayName");
        assertEquals("My Display Name", t.getMethodDisplayName());
        assertTrue(t.getTags().getMethod().contains("MethodTag"));
        assertTrue(t.getTags().getClazz().contains("ClassTag"));
        assertTrue(t.getTags().getInherited().contains("ParentTag"));
        assertEquals("TEST", t.getTestType());
        assertTrue(t.getTestCaseId().contains("Child#myTest") || t.getTestCaseId().contains("Child$"),
                "testCaseId should contain class path and method");
    }

    @Test
    @DisplayName("Should use method name when display name absent")
    void shouldFallbackToMethodName() throws Exception {
        ExtensionContext root = mock(ExtensionContext.class);
        when(root.getParent()).thenReturn(Optional.empty());
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        Method m = Child.class.getDeclaredMethod("noDisplay");
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getRoot()).thenReturn(root);
        when(ctx.getParent()).thenReturn(Optional.empty());
        when(ctx.getTestClass()).thenReturn(Optional.of(Child.class));
        when(ctx.getRequiredTestClass()).thenReturn((Class) Child.class);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));
        when(ctx.getRequiredTestMethod()).thenReturn(m);
        when(ctx.getDisplayName()).thenReturn("noDisplay()"); // auto-generated

        QAPJunitExtension ext = new QAPJunitExtension();
        ext.beforeEach(ctx);

        QAPTest t = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertNotNull(t);
        assertEquals("noDisplay", t.getDisplayName());
    }

    @Test
    @DisplayName("Should generate correct testCaseId for normal test")
    void shouldGenerateTestCaseIdForNormalTest() throws Exception {
        ExtensionContext root = mock(ExtensionContext.class);
        when(root.getParent()).thenReturn(Optional.empty());
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        Method m = MyClass.class.getDeclaredMethod("myTest");
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getRoot()).thenReturn(root);
        when(ctx.getTestClass()).thenReturn(Optional.of(MyClass.class));
        when(ctx.getRequiredTestClass()).thenReturn((Class) MyClass.class);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));
        when(ctx.getRequiredTestMethod()).thenReturn(m);
        when(ctx.getDisplayName()).thenReturn("myTest()");

        new QAPJunitExtension().beforeEach(ctx);
        QAPTest t = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertTrue(t.getTestCaseId().endsWith("MyClass#myTest"));
    }

    @Test
    @DisplayName("Should generate correct testCaseId for parameterized test")
    void shouldGenerateTestCaseIdForParameterizedTest() throws Throwable {
        // Arrange stores and seed QAPTest
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        ExtensionContext root = mock(ExtensionContext.class);
        when(ctx.getRoot()).thenReturn(root);
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        Method m = MyClass.class.getDeclaredMethod("myTest");
        when(ctx.getRequiredTestMethod()).thenReturn(m);
        when(ctx.getRequiredTestClass()).thenReturn((Class) MyClass.class);

        QAPTest test = new QAPTest("myTest", "myTest");
        store.put(METHOD_DESCRIPTION_KEY, test);

        @SuppressWarnings("unchecked")
        ReflectiveInvocationContext<Method> ric = mock(ReflectiveInvocationContext.class);
        when(ric.getArguments()).thenReturn(List.of("A"));
        InvocationInterceptor.Invocation<Void> invocation = mock(InvocationInterceptor.Invocation.class);

        QAPJunitMethodInterceptor interceptor = new QAPJunitMethodInterceptor(new ConcurrentHashMap<>());

        // Act
        interceptor.interceptTestTemplateMethod(invocation, ric, ctx);

        // Assert
        QAPTest t = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertTrue(t.getTestCaseId().endsWith("MyClass#myTest[0]"));
        assertEquals("PARAMETERIZED", t.getTestType());
    }

    @Test
    @DisplayName("Should generate correct testCaseId for nested test")
    void shouldGenerateTestCaseIdForNestedTest() throws Exception {
        ExtensionContext root = mock(ExtensionContext.class);
        when(root.getParent()).thenReturn(Optional.empty());
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        Method m = Outer.Inner.class.getDeclaredMethod("myTest");
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getRoot()).thenReturn(root);
        when(ctx.getTestClass()).thenReturn(Optional.of(Outer.Inner.class));
        when(ctx.getRequiredTestClass()).thenReturn((Class) Outer.Inner.class);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));
        when(ctx.getRequiredTestMethod()).thenReturn(m);
        when(ctx.getDisplayName()).thenReturn("myTest()");

        new QAPJunitExtension().beforeEach(ctx);
        QAPTest t = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertTrue(t.getTestCaseId().endsWith("Outer$Inner#myTest"));
    }

    @Test
    @DisplayName("Should capture parameters with correct types")
    void shouldCaptureParametersWithTypes() throws Throwable {
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        ExtensionContext root = mock(ExtensionContext.class);
        when(ctx.getRoot()).thenReturn(root);
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        Method m = ParamHolder.class.getDeclaredMethod("myTest", String.class, int.class, boolean.class);
        when(ctx.getRequiredTestMethod()).thenReturn(m);
        when(ctx.getRequiredTestClass()).thenReturn((Class) ParamHolder.class);

        QAPTest seed = new QAPTest("myTest", "myTest");
        store.put(METHOD_DESCRIPTION_KEY, seed);

        @SuppressWarnings("unchecked")
        ReflectiveInvocationContext<Method> ric = mock(ReflectiveInvocationContext.class);
        when(ric.getArguments()).thenReturn(List.of("S", 5, true));
        InvocationInterceptor.Invocation<Void> invocation = mock(InvocationInterceptor.Invocation.class);

        QAPJunitMethodInterceptor interceptor = new QAPJunitMethodInterceptor(new ConcurrentHashMap<>());
        interceptor.interceptTestTemplateMethod(invocation, ric, ctx);

        QAPTest t = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertEquals(3, t.getParameters().size());
        assertEquals("String", t.getParameters().get(0).argumentType());
        assertEquals("Integer", t.getParameters().get(1).argumentType());
        assertEquals("Boolean", t.getParameters().get(2).argumentType());
    }

    @Test
    @DisplayName("Should calculate duration correctly")
    void shouldCalculateDuration() {
        QAPTest t = new QAPTest("m", "m");
        t.setStartTime(1000);
        t.setEndTime(1050);
        assertEquals(50, t.getDurationMillis());
    }

    @Test
    @DisplayName("Should set testType to TEST for normal tests")
    void shouldSetTestTypeForNormalTest() throws Exception {
        ExtensionContext root = mock(ExtensionContext.class);
        when(root.getParent()).thenReturn(Optional.empty());
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        Method m = MyClass.class.getDeclaredMethod("myTest");
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getRoot()).thenReturn(root);
        when(ctx.getTestClass()).thenReturn(Optional.of(MyClass.class));
        when(ctx.getRequiredTestClass()).thenReturn((Class) MyClass.class);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));
        when(ctx.getRequiredTestMethod()).thenReturn(m);
        when(ctx.getDisplayName()).thenReturn("myTest()");

        new QAPJunitExtension().beforeEach(ctx);
        QAPTest t = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertEquals("TEST", t.getTestType());
    }

    @Test
    @DisplayName("Should set testType to PARAMETERIZED for parameterized tests")
    void shouldSetTestTypeForParameterizedTest() throws Throwable {
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        ExtensionContext root = mock(ExtensionContext.class);
        when(ctx.getRoot()).thenReturn(root);
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        Method m = MyClass.class.getDeclaredMethod("myTest");
        when(ctx.getRequiredTestMethod()).thenReturn(m);
        when(ctx.getRequiredTestClass()).thenReturn((Class) MyClass.class);

        QAPTest seed = new QAPTest("myTest", "myTest");
        store.put(METHOD_DESCRIPTION_KEY, seed);

        @SuppressWarnings("unchecked")
        ReflectiveInvocationContext<Method> ric = mock(ReflectiveInvocationContext.class);
        when(ric.getArguments()).thenReturn(List.of("A"));
        InvocationInterceptor.Invocation<Void> invocation = mock(InvocationInterceptor.Invocation.class);

        QAPJunitMethodInterceptor interceptor = new QAPJunitMethodInterceptor(new ConcurrentHashMap<>());
        interceptor.interceptTestTemplateMethod(invocation, ric, ctx);

        QAPTest t = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertEquals("PARAMETERIZED", t.getTestType());
    }
}
