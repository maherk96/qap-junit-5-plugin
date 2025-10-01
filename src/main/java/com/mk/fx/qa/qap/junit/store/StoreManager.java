package com.mk.fx.qa.qap.junit.store;

import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;


public class StoreManager {

    private StoreManager() {
        // static only
    }

    public static ExtensionContext.Store getMethodStore(ExtensionContext context) {
        return context
                .getRoot()
                .getStore(
                        ExtensionContext.Namespace.create(
                                QAPJunitExtension.class, context.getRequiredTestMethod()));
    }

    public static ExtensionContext.Store getClassStore(ExtensionContext context) {
        return context
                .getRoot()
                .getStore(
                        ExtensionContext.Namespace.create(
                                QAPJunitExtension.class, context.getRequiredTestClass()));
    }

    public static <T> T getMethodStoreData(ExtensionContext context, String key, Class<T> type) {
        return getMethodStore(context).get(key, type);
    }

    public static <T> T getClassStoreData(ExtensionContext context, String key, Class<T> type) {
        return getClassStore(context).get(key, type);
    }

    public static void putMethodStoreData(ExtensionContext context, String key, Object value) {
        getMethodStore(context).put(key, value);
    }

    public static void putClassStoreData(ExtensionContext context, String key, Object value) {
        getClassStore(context).put(key, value);
    }

    public static void addDescriptionToClassStore(ExtensionContext context, QAPTest qapTest) {
        ExtensionContext.Store classStore = getClassStore(context);
        var qapTests =
                classStore.getOrDefault(
                        METHOD_DESCRIPTION_KEY, ArrayList.class, new ArrayList<QAPTest>());
        qapTests.add(qapTest);
        classStore.put(METHOD_DESCRIPTION_KEY, qapTests);
    }
}