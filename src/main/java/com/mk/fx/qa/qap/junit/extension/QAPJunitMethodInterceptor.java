package com.mk.fx.qa.qap.junit.extension;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.*;

import com.mk.fx.qa.qap.junit.core.QAPUtils;
import com.mk.fx.qa.qap.junit.model.QAPFailure;
import com.mk.fx.qa.qap.junit.model.QAPFixture;
import com.mk.fx.qa.qap.junit.model.QAPParameterization;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.model.QAPTestClass;
import com.mk.fx.qa.qap.junit.model.QAPTestParams;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import com.mk.fx.qa.qap.junit.util.ExceptionFormatter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

public class QAPJunitMethodInterceptor implements IMethodInterceptor {
  private final Map<String, Throwable> failedInits;

  public QAPJunitMethodInterceptor(Map<String, Throwable> failedInits) {
    this.failedInits = failedInits;
  }

  @Override
  public void interceptTestTemplateMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    var testParams = invocationContext.getArguments().toArray();
    var qapTest =
        StoreManager.getMethodStoreData(extensionContext, METHOD_DESCRIPTION_KEY, QAPTest.class);
    
    Method method = extensionContext.getRequiredTestMethod();
    Parameter[] parameters = method.getParameters();
    
    List<QAPTestParams> qapTestParams = new ArrayList<>();
    for (int i = 0; i < testParams.length; i++) {
      var arg = testParams[i];
      var argClassName = (arg != null) ? arg.getClass().getSimpleName() : "null";
      var argStringValue = (arg != null) ? arg.toString() : "null";
      
      // Extract parameter name if available (requires -parameters compiler flag)
      String paramName = null;
      if (i < parameters.length) {
        Parameter param = parameters[i];
        paramName = param.getName();
        // If parameter name is synthetic (arg0, arg1, etc.), it means debug info is not available
        // Check if it's a synthetic name by checking if it matches the pattern arg\d+
        if (paramName != null && paramName.matches("arg\\d+")) {
          // Synthetic name - debug info not available, set to null
          paramName = null;
        }
      }
      
      var params = new QAPTestParams(i, paramName, argClassName, argStringValue);
      qapTestParams.add(params);
    }
    qapTest.setParameters(qapTestParams);
    qapTest.setTestType("PARAMETERIZED");

    // Extract parameterization source/provider
    String provider = extractParameterizationProvider(method);
    
    // Compute a stable index per method invocation using the method-level store
    var methodStore = StoreManager.getMethodStore(extensionContext);
    Integer current = methodStore.get(QAPUtils.PARAM_INDEX_KEY, Integer.class);
    int index = (current == null) ? 0 : current + 1;
    methodStore.put(QAPUtils.PARAM_INDEX_KEY, index);
    
    // Set parameterization metadata
    if (provider != null) {
      qapTest.setParameterization(new QAPParameterization(provider, index));
    }

    // Build testCaseId as TopLevelClass#methodName[index]
    String fqcn = extensionContext.getRequiredTestClass().getName();
    String nestedPath = fqcn.substring(fqcn.lastIndexOf('.') + 1);
    String id =
        nestedPath + "#" + extensionContext.getRequiredTestMethod().getName() + "[" + index + "]";
    qapTest.setTestCaseId(id);
    invocation.proceed();
  }

  /**
   * Extracts the parameterization provider name from method annotations.
   * Checks for common JUnit 5 parameterization annotations.
   */
  private String extractParameterizationProvider(Method method) {
    if (method.getAnnotation(CsvSource.class) != null) {
      return "CsvSource";
    }
    if (method.getAnnotation(MethodSource.class) != null) {
      return "MethodSource";
    }
    if (method.getAnnotation(ValueSource.class) != null) {
      return "ValueSource";
    }
    if (method.getAnnotation(ArgumentsSource.class) != null) {
      return "ArgumentsSource";
    }
    if (method.getAnnotation(EnumSource.class) != null) {
      return "EnumSource";
    }
    if (method.getAnnotation(NullSource.class) != null) {
      return "NullSource";
    }
    if (method.getAnnotation(EmptySource.class) != null) {
      return "EmptySource";
    }
    if (method.getAnnotation(NullAndEmptySource.class) != null) {
      return "NullAndEmptySource";
    }
    // Check for composite sources (multiple annotations)
    if (method.getAnnotations().length > 0) {
      // Look for any annotation that might be a parameter source
      for (var annotation : method.getAnnotations()) {
        String annotationName = annotation.annotationType().getSimpleName();
        if (annotationName.contains("Source") || annotationName.contains("Provider")) {
          return annotationName;
        }
      }
    }
    return null;
  }

  @Override
  public void interceptBeforeAllMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();
    var classStore = StoreManager.getClassStore(extensionContext);
    classStore.put(FIXTURE_START_TIME_KEY, startTime);
    classStore.put(FIXTURE_START_TIME_NANOS_KEY, startTimeNanos);
    
    Throwable failure = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      failure = t;
      failedInits.put(extensionContext.getUniqueId(), t);
      throw t;
    } finally {
      long endTime = System.currentTimeMillis();
      long endTimeNanos = System.nanoTime();
      long durationMillis = endTime - startTime;
      long durationNanos = endTimeNanos - startTimeNanos;
      
      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;
      
      // Get the fixture method from the invocation context
      Method fixtureMethod = (Method) invocationContext.getExecutable();
      
      // beforeAll is class-level, so we record it at class level
      // (it will be added to all tests in the class when they're created)
      QAPFixture fixture = new QAPFixture(
          "BEFORE_ALL",
          failure != null ? "FAILED" : "PASSED",
          durationMillis,
          durationNanos,
          qapFailure);
      addFixtureToClass(extensionContext, fixture, fixtureMethod);
    }
  }

  @Override
  public void interceptBeforeEachMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();
    var methodStore = StoreManager.getMethodStore(extensionContext);
    methodStore.put(FIXTURE_START_TIME_KEY, startTime);
    methodStore.put(FIXTURE_START_TIME_NANOS_KEY, startTimeNanos);
    
    Throwable failure = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      failure = t;
      // Link BeforeEach failure to the test case
      QAPTest qapTest = StoreManager.getMethodStoreData(extensionContext, METHOD_DESCRIPTION_KEY, QAPTest.class);
      if (qapTest != null) {
        qapTest.setFailure(ExceptionFormatter.toFailure(t));
        qapTest.setStatus("FAILED");
      }
      throw t;
    } finally {
      long endTime = System.currentTimeMillis();
      long endTimeNanos = System.nanoTime();
      long durationMillis = endTime - startTime;
      long durationNanos = endTimeNanos - startTimeNanos;
      
      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;
      
      // Get the fixture method from the invocation context
      Method fixtureMethod = (Method) invocationContext.getExecutable();
      
      // Add fixture to the current test case's lifecycle
      addFixtureToTest(extensionContext, "BEFORE_EACH", fixtureMethod, 
          failure != null ? "FAILED" : "PASSED", durationNanos, qapFailure);
    }
  }

  @Override
  public void interceptAfterEachMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();
    
    Throwable failure = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      failure = t;
      throw t;
    } finally {
      long endTime = System.currentTimeMillis();
      long endTimeNanos = System.nanoTime();
      long durationMillis = endTime - startTime;
      long durationNanos = endTimeNanos - startTimeNanos;
      
      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;
      
      // Get the fixture method from the invocation context
      Method fixtureMethod = (Method) invocationContext.getExecutable();
      
      // Add fixture to the current test case's lifecycle
      addFixtureToTest(extensionContext, "AFTER_EACH", fixtureMethod,
          failure != null ? "FAILED" : "PASSED", durationNanos, qapFailure);
    }
  }

  @Override
  public void interceptAfterAllMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    long startTime = System.currentTimeMillis();
    long startTimeNanos = System.nanoTime();
    var classStore = StoreManager.getClassStore(extensionContext);
    classStore.put(FIXTURE_START_TIME_KEY, startTime);
    classStore.put(FIXTURE_START_TIME_NANOS_KEY, startTimeNanos);
    
    Throwable failure = null;
    try {
      invocation.proceed();
    } catch (Throwable t) {
      failure = t;
      throw t;
    } finally {
      long endTime = System.currentTimeMillis();
      long endTimeNanos = System.nanoTime();
      long durationMillis = endTime - startTime;
      long durationNanos = endTimeNanos - startTimeNanos;
      
      QAPFailure qapFailure = failure != null ? ExceptionFormatter.toFailure(failure) : null;
      
      // Get the fixture method from the invocation context
      Method fixtureMethod = (Method) invocationContext.getExecutable();
      
      // afterAll is class-level, so we record it at class level
      QAPFixture fixture = new QAPFixture(
          "AFTER_ALL",
          failure != null ? "FAILED" : "PASSED",
          durationMillis,
          durationNanos,
          qapFailure);
      addFixtureToClass(extensionContext, fixture, fixtureMethod);
    }
  }

  /**
   * Adds a fixture to the current test case's lifecycle.
   * This links fixtures directly to the test that was running when they executed.
   */
  private void addFixtureToTest(
      ExtensionContext context,
      String phase,
      Method fixtureMethod,
      String status,
      long durationNanos,
      QAPFailure error) {
    // Get the current test case from method store
    QAPTest qapTest = StoreManager.getMethodStoreData(context, METHOD_DESCRIPTION_KEY, QAPTest.class);
    if (qapTest == null) {
      // No test case available (e.g., beforeAll/afterAll at class level)
      // Fall back to class-level recording
      QAPFixture fixture = new QAPFixture(phase, status, 0L, durationNanos, error);
      addFixtureToClass(context, fixture, fixtureMethod);
      return;
    }

    // Ensure lifecycle is initialized
    if (qapTest.getLifecycle() == null) {
      qapTest.setLifecycle(new com.mk.fx.qa.qap.junit.model.QAPTestLifecycle());
    }

    // Create test fixture with method and class information
    String methodName = fixtureMethod.getName();
    String className = fixtureMethod.getDeclaringClass().getSimpleName();
    
    // Determine order based on current list size
    int order;
    java.util.List<com.mk.fx.qa.qap.junit.model.QAPTestFixture> targetList;
    if ("BEFORE_EACH".equals(phase)) {
      targetList = qapTest.getLifecycle().getBeforeEach();
      order = targetList.size() + 1;
    } else if ("AFTER_EACH".equals(phase)) {
      targetList = qapTest.getLifecycle().getAfterEach();
      order = targetList.size() + 1;
    } else if ("BEFORE_ALL".equals(phase)) {
      targetList = qapTest.getLifecycle().getBeforeAll();
      order = targetList.size() + 1;
    } else if ("AFTER_ALL".equals(phase)) {
      targetList = qapTest.getLifecycle().getAfterAll();
      order = targetList.size() + 1;
    } else {
      return; // Unknown phase
    }

    com.mk.fx.qa.qap.junit.model.QAPTestFixture testFixture =
        new com.mk.fx.qa.qap.junit.model.QAPTestFixture(
            methodName, className, order, status, durationNanos, error);
    targetList.add(testFixture);
  }

  /**
   * Legacy method for class-level fixtures (beforeAll/afterAll when no test is running).
   * Kept for backward compatibility and class-level fixtures.
   */
  private void addFixtureToClass(ExtensionContext context, QAPFixture fixture, Method fixtureMethod) {
    var classStore = StoreManager.getClassStore(context);
    @SuppressWarnings("unchecked")
    Map<String, QAPTestClass> nodes =
        classStore.getOrDefault(CLASS_NODES_KEY, Map.class, new java.util.concurrent.ConcurrentHashMap<>());
    
    // Record fixture in the test class where the test is currently running
    Class<?> testClass = context.getRequiredTestClass();
    String testClassKey = testClass.getName();
    
    QAPTestClass testClassNode = nodes.computeIfAbsent(testClassKey, k -> {
      QAPTestClass newNode = new QAPTestClass(
          testClass.getSimpleName(),
          testClass.getSimpleName(),
          java.util.Collections.emptySet());
      newNode.setClassFqn(testClassKey);
      newNode.setClassSimpleName(testClass.getSimpleName());
      newNode.setClassKey(testClassKey);
      newNode.setFixtures(new ArrayList<>());
      return newNode;
    });
    
    // Ensure fixtures list is initialized
    if (testClassNode.getFixtures() == null) {
      testClassNode.setFixtures(new ArrayList<>());
    }
    
    // Record fixture - each fixture execution is recorded in the test class
    testClassNode.getFixtures().add(fixture);
    nodes.put(testClassKey, testClassNode);
    classStore.put(CLASS_NODES_KEY, nodes);
  }
}
