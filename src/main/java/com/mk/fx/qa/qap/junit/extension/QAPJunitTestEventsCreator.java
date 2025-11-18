package com.mk.fx.qa.qap.junit.extension;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.TEST_CLASS_DATA_KEY;

import com.mk.fx.qa.qap.junit.core.QAPUtils;
import com.mk.fx.qa.qap.junit.core.TestCaseStatus;
import com.mk.fx.qa.qap.junit.model.QAPHeader;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.model.QAPTestClass;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.extension.ExtensionContext;

public class QAPJunitTestEventsCreator implements ITestEventCreator {

  private static final String SYSTEM_PROPERTY_LAUNCH_ID = "launchID";

  @Override
  public void addTestEventsToTestLaunch(ExtensionContext context, QAPJunitLaunch launch) {
    @SuppressWarnings("unchecked")
    java.util.Map<String, com.mk.fx.qa.qap.junit.model.QAPTestClass> nodes =
        StoreManager.getClassStoreData(context, QAPUtils.CLASS_NODES_KEY, java.util.Map.class);
    if (nodes == null || nodes.isEmpty()) {
      // Fallback to legacy flat list
      @SuppressWarnings("unchecked")
      java.util.List<com.mk.fx.qa.qap.junit.model.QAPTest> events =
          StoreManager.getClassStoreData(
              context, QAPUtils.METHOD_DESCRIPTION_KEY, java.util.List.class);
      com.mk.fx.qa.qap.junit.model.QAPTestClass launchRoot =
          launch.getTestClasses().isEmpty() ? null : launch.getTestClasses().get(0);
      if (launchRoot == null) {
        var clazz = context.getRequiredTestClass();
        launchRoot =
            new com.mk.fx.qa.qap.junit.model.QAPTestClass(
                clazz.getSimpleName(), context.getDisplayName(), java.util.Collections.emptySet());
        launch.getTestClasses().add(launchRoot);
      }
      launchRoot.setTestCases(events != null ? events : new java.util.ArrayList<>());
      return;
    }

    // Prepare a single root class entry from the launch
    com.mk.fx.qa.qap.junit.model.QAPTestClass launchRoot =
        launch.getTestClasses().isEmpty() ? null : launch.getTestClasses().get(0);

    // Build a parent-child hierarchy based on classKey tokens split by '$'
    String rootKey = (launchRoot != null) ? launchRoot.getClassKey() : null;
    if (rootKey == null) {
      // Fallback to context top-level
      rootKey = context.getRequiredTestClass().getName();
      if (launchRoot != null) launchRoot.setClassKey(rootKey);
    }

    // Ensure root node metadata is applied to launch's testClass
    com.mk.fx.qa.qap.junit.model.QAPTestClass rootNode = nodes.get(rootKey);
    if (launchRoot == null) {
      launchRoot =
          (rootNode != null)
              ? rootNode
              : new com.mk.fx.qa.qap.junit.model.QAPTestClass(
                  context.getRequiredTestClass().getSimpleName(),
                  context.getDisplayName(),
                  java.util.Collections.emptySet());
      launch.getTestClasses().add(launchRoot);
    }

    if (rootNode != null) {
      launchRoot.setTestCases(rootNode.getTestCases());
      launchRoot.setClassChain(rootNode.getClassChain());
      launchRoot.setInheritedClassTags(rootNode.getInheritedClassTags());
    } else {
      // If no collected tests for root, at least attach an empty list
      launchRoot.setTestCases(new java.util.ArrayList<>());
    }

    // Link children
    java.util.Map<String, com.mk.fx.qa.qap.junit.model.QAPTestClass> attach =
        new java.util.HashMap<>(nodes);
    attach.remove(rootKey); // exclude root

    // Helper to find parent key from a class key
    java.util.function.Function<String, String> parentKey =
        key -> {
          int idx = key.lastIndexOf('$');
          if (idx <= 0) return null;
          return key.substring(0, idx);
        };

    // Build a map of children lists
    java.util.Map<String, java.util.List<com.mk.fx.qa.qap.junit.model.QAPTestClass>> childrenMap =
        new java.util.HashMap<>();
    for (var entry : attach.entrySet()) {
      String k = entry.getKey();
      String pk = parentKey.apply(k);
      if (pk == null) continue; // ignore orphans
      childrenMap.computeIfAbsent(pk, x -> new java.util.ArrayList<>()).add(entry.getValue());
    }

    // Attach recursively
    java.util.function.Consumer<com.mk.fx.qa.qap.junit.model.QAPTestClass> attachChildren =
        new java.util.function.Consumer<>() {
          @Override
          public void accept(com.mk.fx.qa.qap.junit.model.QAPTestClass node) {
            java.util.List<com.mk.fx.qa.qap.junit.model.QAPTestClass> kids =
                childrenMap.get(node.getClassKey());
            if (kids != null) {
              node.setChildren(kids);
              for (var c : kids) accept(c);
            }
          }
        };

    // Ensure we keep the root key during attachment so children map lookup works
    String savedRootKeyForAttachment = launchRoot.getClassKey();
    if (savedRootKeyForAttachment == null) {
      // If missing (e.g., newly created root), use computed rootKey
      launchRoot.setClassKey(rootKey);
    }
    attachChildren.accept(launchRoot);
    // Root should not carry parentClassKey in the final JSON
    launchRoot.setClassKey(null);
  }

  /**
   * @param context provides test specific information and state
   * @param status test status
   * @param t exception if present
   */
  @Override
  public void createTestTemplate(ExtensionContext context, TestCaseStatus status, Throwable t) {
    var qapTest =
        StoreManager.getMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, QAPTest.class);
    qapTest.setEndTime(Instant.now().toEpochMilli());
    qapTest.setStatus(status.name());
    if (t != null) {
      qapTest.setException(t.getMessage().getBytes(StandardCharsets.UTF_8));
    }
  }

  /**
   * @param context provides test specific information and state
   * @return launch for junit test
   */
  @Override
  public QAPJunitLaunch startLaunchQAP(ExtensionContext context) {
    var clazz = context.getRequiredTestClass();
    java.util.Set<String> classTags = new java.util.HashSet<>();
    for (org.junit.jupiter.api.Tag t :
        clazz.getAnnotationsByType(org.junit.jupiter.api.Tag.class)) {
      classTags.add(t.value());
    }
    var rootClass = new QAPTestClass(clazz.getSimpleName(), context.getDisplayName(), classTags);
    var qapLaunch =
        new QAPJunitLaunch(
            new QAPHeader(
                Instant.now().toEpochMilli(), System.getProperty(SYSTEM_PROPERTY_LAUNCH_ID)),
            new java.util.ArrayList<>(java.util.List.of(rootClass)));

    // Populate class-level metadata
    rootClass.setClassKey(clazz.getName());
    String nestedPath = clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
    rootClass.setFullClassName(nestedPath);
    java.util.List<String> chain = new java.util.ArrayList<>();
    // Standard: empty chain for root
    rootClass.setClassChain(chain);
    rootClass.setInheritedClassTags(java.util.Collections.emptySet());

    StoreManager.putClassStoreData(context, TEST_CLASS_DATA_KEY, qapLaunch);
    return qapLaunch;
  }
}
