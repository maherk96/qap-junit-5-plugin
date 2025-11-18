package com.mk.fx.qa.qap.junit.store;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;

import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.extension.ExtensionContext;

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
    Class<?> topLevel = resolveTopLevelTestClass(context);
    return context
        .getRoot()
        .getStore(ExtensionContext.Namespace.create(QAPJunitExtension.class, topLevel));
  }

  /**
   * Resolves the top-level test class for the current context by walking up the parent chain and
   * remembering the last seen test class. This lets us aggregate nested test results under a single
   * class key.
   */
  public static Class<?> resolveTopLevelTestClass(ExtensionContext context) {
    Class<?> top = null;
    ExtensionContext current = context;
    while (current != null) {
      var maybeClass = current.getTestClass();
      if (maybeClass.isPresent()) {
        top = maybeClass.get();
      }
      var parent = current.getParent();
      current = parent.orElse(null);
    }
    return top != null ? top : context.getRequiredTestClass();
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
    @SuppressWarnings("unchecked")
    java.util.Map<String, com.mk.fx.qa.qap.junit.model.QAPTestClass> nodes =
        classStore.getOrDefault(
            com.mk.fx.qa.qap.junit.core.QAPUtils.CLASS_NODES_KEY,
            java.util.Map.class,
            new java.util.concurrent.ConcurrentHashMap<
                String, com.mk.fx.qa.qap.junit.model.QAPTestClass>());
    String key = context.getRequiredTestClass().getName();
    com.mk.fx.qa.qap.junit.model.QAPTestClass node =
        nodes.computeIfAbsent(
            key,
            k ->
                new com.mk.fx.qa.qap.junit.model.QAPTestClass(
                    context.getRequiredTestClass().getSimpleName(),
                    context.getDisplayName(),
                    java.util.Collections.emptySet()));
    if (node.getTestCases() == null) {
      node.setTestCases(new CopyOnWriteArrayList<>());
    }
    node.getTestCases().add(qapTest);
    nodes.put(key, node);
    classStore.put(com.mk.fx.qa.qap.junit.core.QAPUtils.CLASS_NODES_KEY, nodes);

    // Maintain backward-compatible flat list
    List<QAPTest> flat =
        classStore.getOrDefault(
            METHOD_DESCRIPTION_KEY, List.class, new CopyOnWriteArrayList<QAPTest>());
    flat.add(qapTest);
    classStore.put(METHOD_DESCRIPTION_KEY, flat);
  }
}
