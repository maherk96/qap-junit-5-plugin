package com.mk.fx.qa.qap.junit.extension;

import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

public interface IMethodInterceptor {

  void interceptTestTemplateMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable;

  void interceptBeforeAllMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable;

  void interceptBeforeEachMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable;

  void interceptAfterEachMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable;

  void interceptAfterAllMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable;
}
