
package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.model.QAPTestParams;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import com.mk.fx.qa.qap.junit.core.QAPUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;


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
        var qapTest = StoreManager.getMethodStoreData(extensionContext, METHOD_DESCRIPTION_KEY, QAPTest.class);
        List<QAPTestParams> qapTestParams = new ArrayList<>();
        IntStream.range(0, testParams.length)
                .forEach(args -> {
                    var arg = testParams[args];
                    var argClassName = (arg != null) ? arg.getClass().getSimpleName() : "null";
                    var argStringValue = (arg != null) ? arg.toString() : "null";
                    var params = new QAPTestParams(args, argClassName, argStringValue);
                    qapTestParams.add(params);
                });
        qapTest.setParameters(qapTestParams);
        qapTest.setTestType("PARAMETERIZED");

        // Compute a stable index per method invocation using the method-level store
        var methodStore = StoreManager.getMethodStore(extensionContext);
        Integer current = methodStore.get(QAPUtils.PARAM_INDEX_KEY, Integer.class);
        int index = (current == null) ? 0 : current + 1;
        methodStore.put(QAPUtils.PARAM_INDEX_KEY, index);

        // Build testCaseId as TopLevelClass#methodName[index]
        String fqcn = extensionContext.getRequiredTestClass().getName();
        String nestedPath = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        String id = nestedPath + "#" + extensionContext.getRequiredTestMethod().getName() + "[" + index + "]";
        qapTest.setTestCaseId(id);
        invocation.proceed();
    }

    @Override
    public void interceptBeforeAllMethod(
            InvocationInterceptor.Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext)
            throws Throwable {
        try {
            invocation.proceed();
        } catch (Throwable t) {
            failedInits.put(extensionContext.getUniqueId(), t);
            throw t;
        }
    }
}
