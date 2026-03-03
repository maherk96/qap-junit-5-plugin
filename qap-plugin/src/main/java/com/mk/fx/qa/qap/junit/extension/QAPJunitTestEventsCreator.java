package com.mk.fx.qa.qap.junit.extension;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.TEST_CLASS_DATA_KEY;

import com.mk.fx.qa.qap.junit.core.QAPUtils;
import com.mk.fx.qa.qap.junit.core.TestCaseStatus;
import com.mk.fx.qa.qap.junit.model.QAPHeader;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import com.mk.fx.qa.qap.junit.model.QAPTestClass;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import com.mk.fx.qa.qap.junit.util.CoverageItemExtractor;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Creates and populates QAP test launch objects by aggregating test execution data.
 *
 * <p>This class is responsible for:
 *
 * <ul>
 *   <li>Starting new test launches with initial metadata and header information
 *   <li>Creating test result templates from completed test executions
 *   <li>Aggregating all test data into a hierarchical class structure
 *   <li>Building nested class parent-child relationships based on Java nesting conventions
 * </ul>
 *
 * <p><strong>Hierarchy Building:</strong>
 *
 * <p>The class constructs a tree structure representing nested test classes. Java nested classes
 * are identified by the {@code $} separator in their fully qualified names (e.g., {@code
 * OuterClass$InnerClass}). Both simple test classes and nested class hierarchies use the same
 * hierarchical processing path. The algorithm:
 *
 * <ol>
 *   <li>Identifies the root test class (top-level class)
 *   <li>Filters tests to separate root class tests from nested class tests
 *   <li>Groups nested classes by their parent using the {@code $} separator
 *   <li>Recursively attaches children to their parents
 *   <li>Limits recursion depth to 10 levels (defensive programming)
 * </ol>
 *
 * <p><strong>Fallback Mode:</strong>
 *
 * <p>In rare edge cases where class metadata is missing from the extension context store (failed
 * initialization, backward compatibility with older versions, or store corruption), the system
 * falls back to creating a minimal launch with a flat test list. This ensures test execution
 * completes even if metadata collection failed.
 *
 * <p><strong>Important:</strong> The fallback mode is NOT related to class nesting. Both simple
 * classes and nested class hierarchies use the normal hierarchical path when metadata is available.
 * The fallback is a safety mechanism for exceptional circumstances that should rarely occur in
 * normal operation.
 *
 * <p><strong>Thread Safety:</strong>
 *
 * <p>This class is designed to be used within JUnit's extension context, which provides thread-safe
 * storage. Methods are not synchronized as they operate on extension context stores that handle
 * concurrency.
 *
 * @see QAPJunitLaunch
 * @see QAPTestClass
 * @see QAPTest
 * @see ITestEventCreator
 * @see ExtensionContext
 */
public class QAPJunitTestEventsCreator implements ITestEventCreator {

  private static final String SYSTEM_PROPERTY_LAUNCH_ID =
      com.mk.fx.qa.qap.junit.core.SystemProperties.LAUNCH_ID;

  /** Separator used in Java nested class names (e.g., OuterClass$InnerClass). */
  private static final String NESTED_CLASS_SEPARATOR = "$";

  /**
   * Aggregates all test execution data and builds hierarchical class structure for the launch.
   *
   * <p>This is the main aggregation method that combines test results from all executed tests into
   * the final launch structure. The method operates in two modes:
   *
   * <ul>
   *   <li><strong>Normal mode:</strong> When class nodes are available in the store, builds a tree
   *       structure representing test classes with proper parent-child relationships (handles both
   *       simple and nested classes)
   *   <li><strong>Fallback mode:</strong> When class nodes are unavailable (rare edge case),
   *       creates a minimal launch with a flat test list
   * </ul>
   *
   * <p><strong>Normal Mode Process:</strong>
   *
   * <ol>
   *   <li>Initialize or retrieve the root test class
   *   <li>Populate root class metadata (tests, fixtures, tags, class chain)
   *   <li>Build parent-child relationships for all nested classes
   *   <li>Filter tests to ensure each test appears only in its declaring class
   *   <li>Recursively attach children to create full tree structure
   * </ol>
   *
   * <p><strong>Important Notes:</strong>
   *
   * <ul>
   *   <li>The root class temporarily gets a classKey during hierarchy building but it's removed
   *       before completion (root should not have classKey in final JSON)
   *   <li>Collections from store nodes are shared references (not defensive copies) for performance
   *   <li>The launch parameter is mutated in place - test classes are added to it
   * </ul>
   *
   * @param context provides access to extension context store containing test execution data
   * @param launch the launch object to populate with aggregated test data
   * @throws IllegalArgumentException if context or launch is null
   */
  @Override
  public void addTestEventsToTestLaunch(ExtensionContext context, QAPJunitLaunch launch) {
    if (context == null) {
      throw new IllegalArgumentException("ExtensionContext cannot be null");
    }
    if (launch == null) {
      throw new IllegalArgumentException("QAPJunitLaunch cannot be null");
    }

    Map<String, QAPTestClass> nodes = retrieveClassNodes(context);

    if (shouldUseFallbackMode(nodes)) {
      populateFallbackLaunch(context, launch);
      return;
    }

    QAPTestClass rootClass = initializeRootClass(context, launch, nodes);
    populateRootClassData(rootClass, nodes);
    buildClassHierarchy(rootClass, nodes);
  }

  /**
   * Creates a test result template by recording test completion data and lifecycle duration.
   *
   * <p>This method is called when a test completes (success, failure, or abort). It:
   *
   * <ul>
   *   <li>Records test end time (milliseconds and nanoseconds)
   *   <li>Sets final test status (PASSED, FAILED, ABORTED, SKIPPED)
   *   <li>Captures failure details if test threw exception
   *   <li>Records test execution duration in lifecycle for detailed timing
   * </ul>
   *
   * <p><strong>Lifecycle Recording:</strong>
   *
   * <p>If the test has lifecycle tracking enabled, this method creates a TestExecution entry in the
   * lifecycle and populates its duration. This allows for separate timing of test method execution
   * vs. fixture overhead.
   *
   * <p><strong>Failure Handling:</strong>
   *
   * <p>When a test fails, the exception is formatted into a QAPFailure object containing:
   *
   * <ul>
   *   <li>Exception class name and message
   *   <li>Full stack trace (formatted according to configured limits)
   *   <li>Cause chain (if present)
   * </ul>
   *
   * @param context provides access to method store containing test data
   * @param status final test case status (PASSED, FAILED, ABORTED, SKIPPED)
   * @param throwable the exception thrown by test, or null if test passed
   * @throws IllegalArgumentException if context or status is null
   */
  @Override
  public void createTestTemplate(
      ExtensionContext context, TestCaseStatus status, Throwable throwable) {
    if (context == null) {
      throw new IllegalArgumentException("ExtensionContext cannot be null");
    }
    if (status == null) {
      throw new IllegalArgumentException("TestCaseStatus cannot be null");
    }

    QAPTest qapTest =
        StoreManager.getMethodStoreData(context, QAPUtils.METHOD_DESCRIPTION_KEY, QAPTest.class);
    if (qapTest == null) {
      return;
    }

    qapTest.setEndTime(Instant.now().toEpochMilli());
    qapTest.setEndTimeNanos(System.nanoTime());
    qapTest.setStatus(status.name());

    if (throwable != null) {
      qapTest.setFailure(com.mk.fx.qa.qap.junit.util.ExceptionFormatter.toFailure(throwable));
    }

    if (qapTest.getLifecycle() != null) {
      if (qapTest.getLifecycle().getTest() == null) {
        qapTest
            .getLifecycle()
            .setTest(new com.mk.fx.qa.qap.junit.model.QAPTestLifecycle.TestExecution());
      }
      qapTest.getLifecycle().getTest().setDurationNanos(qapTest.getDurationNanos());
    }
  }

  /**
   * Starts a new QAP test launch and initializes the root class with metadata.
   *
   * <p>This method is called at the beginning of test execution (typically in {@code beforeAll}).
   * It:
   *
   * <ul>
   *   <li>Creates launch header with timestamp and launch ID
   *   <li>Collects class-level tags from {@code @Tag} annotations
   *   <li>Initializes root test class node with metadata
   *   <li>Stores launch in extension context for later population
   * </ul>
   *
   * <p><strong>Root Class Metadata:</strong>
   *
   * <p>The root class is populated with:
   *
   * <ul>
   *   <li><strong>classKey:</strong> Fully qualified class name (e.g., com.example.MyTest)
   *   <li><strong>classFqn:</strong> Same as classKey
   *   <li><strong>classSimpleName:</strong> Simple class name (e.g., MyTest)
   *   <li><strong>classChain:</strong> Empty list (root has no parent classes)
   *   <li><strong>inheritedClassTags:</strong> Empty set (root doesn't inherit tags)
   *   <li><strong>fixtures:</strong> Empty list (populated later as fixtures execute)
   * </ul>
   *
   * <p><strong>Launch ID:</strong>
   *
   * <p>The launch ID is read from system property {@code qap.launch.id} and allows correlating test
   * results across multiple test classes in the same test run.
   *
   * @param context provides access to test class information and extension store
   * @return newly created launch with initialized header and root class
   * @throws IllegalArgumentException if context is null
   */
  @Override
  public QAPJunitLaunch startLaunchQAP(ExtensionContext context) {
    if (context == null) {
      throw new IllegalArgumentException("ExtensionContext cannot be null");
    }

    Class<?> testClass = context.getRequiredTestClass();

    Set<String> classTags = new HashSet<>();
    for (org.junit.jupiter.api.Tag tag :
        testClass.getAnnotationsByType(org.junit.jupiter.api.Tag.class)) {
      classTags.add(tag.value());
    }

    QAPTestClass rootClass =
        new QAPTestClass(testClass.getSimpleName(), context.getDisplayName(), classTags);

    QAPJunitLaunch qapLaunch =
        new QAPJunitLaunch(
            new QAPHeader(
                Instant.now().toEpochMilli(), System.getProperty(SYSTEM_PROPERTY_LAUNCH_ID)),
            new ArrayList<>(List.of(rootClass)));

    rootClass.setClassKey(testClass.getName());
    rootClass.setClassFqn(testClass.getName());
    rootClass.setClassSimpleName(testClass.getSimpleName());
    rootClass.setClassChain(new ArrayList<>());
    rootClass.setInheritedClassTags(Collections.emptySet());
    rootClass.setFixtures(new ArrayList<>());

    StoreManager.putClassStoreData(context, TEST_CLASS_DATA_KEY, qapLaunch);
    return qapLaunch;
  }

  /**
   * Retrieves the class nodes map from the extension context store with type safety.
   *
   * <p>Validates the retrieved object is actually a Map before casting to prevent runtime
   * ClassCastException.
   *
   * @param context the extension context
   * @return map of class key to QAPTestClass, or null if not found or wrong type
   */
  @SuppressWarnings("unchecked")
  private Map<String, QAPTestClass> retrieveClassNodes(ExtensionContext context) {
    try {
      Object data =
          StoreManager.getClassStoreData(context, QAPUtils.CLASS_NODES_KEY, java.util.Map.class);
      if (data instanceof Map) {
        return (Map<String, QAPTestClass>) data;
      }
      return null;
    } catch (ClassCastException e) {
      return null;
    }
  }

  /**
   * Retrieves the fallback test list from the extension context store with type safety.
   *
   * <p>Used only in fallback mode when hierarchical class structure is unavailable. Validates the
   * retrieved object is actually a List before casting.
   *
   * @param context the extension context
   * @return list of QAPTest objects, or null if not found or wrong type
   */
  @SuppressWarnings("unchecked")
  private List<QAPTest> retrieveFallbackTestList(ExtensionContext context) {
    try {
      Object data =
          StoreManager.getClassStoreData(
              context, QAPUtils.METHOD_DESCRIPTION_KEY, java.util.List.class);
      if (data instanceof List) {
        return (List<QAPTest>) data;
      }
      return null;
    } catch (ClassCastException e) {
      return null;
    }
  }

  /**
   * Determines if fallback mode should be used due to missing class metadata.
   *
   * <p>In normal operation, CLASS_NODES_KEY is always populated by {@code beforeAll} in
   * QAPJunitExtension. Fallback mode handles rare edge cases:
   *
   * <ul>
   *   <li>Extension initialization failed or was skipped
   *   <li>Backward compatibility with older data format that didn't populate CLASS_NODES_KEY
   *   <li>Extension context store corruption
   * </ul>
   *
   * <p><strong>Note:</strong> This is NOT related to nested vs. non-nested classes. Both simple and
   * nested class structures use the normal hierarchical path when metadata is available.
   *
   * @param nodes the class nodes map from store
   * @return true if fallback mode should be used (rare edge case)
   */
  private boolean shouldUseFallbackMode(Map<String, QAPTestClass> nodes) {
    return nodes == null || nodes.isEmpty();
  }

  /**
   * Fallback handler for missing class metadata (rare edge case).
   *
   * <p>Creates minimal launch structure when CLASS_NODES_KEY wasn't populated. This is a safety net
   * to ensure test execution completes even if metadata collection failed. In normal operation, the
   * hierarchical path is always used.
   *
   * @param context the extension context
   * @param launch the launch to populate
   */
  private void populateFallbackLaunch(ExtensionContext context, QAPJunitLaunch launch) {
    List<QAPTest> events = retrieveFallbackTestList(context);

    QAPTestClass rootClass =
        launch.getTestClasses().isEmpty() ? null : launch.getTestClasses().get(0);

    if (rootClass == null) {
      Class<?> testClass = context.getRequiredTestClass();
      rootClass =
          new QAPTestClass(
              testClass.getSimpleName(), context.getDisplayName(), Collections.emptySet());
      launch.getTestClasses().add(rootClass);
    }

    rootClass.setTestCases(events != null ? events : new ArrayList<>());
  }

  /**
   * Initializes or retrieves the root class for the launch.
   *
   * <p>The root class represents the top-level test class. This method ensures one exists in the
   * launch and sets its classKey temporarily for hierarchy building (removed later).
   *
   * @param context the extension context
   * @param launch the launch being populated
   * @param nodes all class nodes from store
   * @return the root class (existing or newly created)
   */
  private QAPTestClass initializeRootClass(
      ExtensionContext context, QAPJunitLaunch launch, Map<String, QAPTestClass> nodes) {
    QAPTestClass rootClass =
        launch.getTestClasses().isEmpty() ? null : launch.getTestClasses().get(0);

    String rootKey = (rootClass != null) ? rootClass.getClassKey() : null;
    if (rootKey == null) {
      rootKey = context.getRequiredTestClass().getName();
      if (rootClass != null) {
        rootClass.setClassKey(rootKey);
      }
    }

    if (rootClass == null) {
      QAPTestClass rootNode = nodes.get(rootKey);
      if (rootNode != null) {
        rootClass = rootNode;
      } else {
        rootClass =
            new QAPTestClass(
                context.getRequiredTestClass().getSimpleName(),
                context.getDisplayName(),
                Collections.emptySet());
      }
      launch.getTestClasses().add(rootClass);
    }

    if (rootClass.getClassKey() == null) {
      rootClass.setClassKey(rootKey);
    }

    return rootClass;
  }

  /**
   * Populates the root class with its metadata and filtered tests.
   *
   * <p>Copies metadata from the store node to the root class. Filters tests to only include those
   * belonging directly to the root class (tests from nested classes are attached to their
   * respective nested class nodes).
   *
   * @param rootClass the root class to populate
   * @param nodes all class nodes from store
   */
  private void populateRootClassData(QAPTestClass rootClass, Map<String, QAPTestClass> nodes) {
    String rootKey = rootClass.getClassKey();
    QAPTestClass rootNode = nodes.get(rootKey);

    if (rootNode != null) {
      List<QAPTest> rootTests = filterRootClassTests(rootNode.getTestCases());
      rootClass.setTestCases(rootTests);
      rootClass.setClassChain(rootNode.getClassChain());
      rootClass.setInheritedClassTags(rootNode.getInheritedClassTags());
      rootClass.setFixtures(rootNode.getFixtures());
    } else {
      rootClass.setTestCases(new ArrayList<>());
    }
  }

  /**
   * Builds the nested class hierarchy by attaching children recursively.
   *
   * <p>Creates parent-child relationships based on class key patterns (using {@code $} separator).
   * After building hierarchy, removes the classKey from root (not needed in final JSON).
   *
   * @param rootClass the root class to build hierarchy from
   * @param nodes all class nodes from store
   */
  private void buildClassHierarchy(QAPTestClass rootClass, Map<String, QAPTestClass> nodes) {
    String rootKey = rootClass.getClassKey();

    Map<String, QAPTestClass> nestedClasses = new HashMap<>(nodes);
    nestedClasses.remove(rootKey);

    Map<String, List<QAPTestClass>> childrenMap = buildChildrenMap(nestedClasses);

    attachChildrenRecursively(rootClass, childrenMap, 0);

    rootClass.setClassKey(null);
  }

  /**
   * Filters tests to only include those from the root class (not nested classes).
   *
   * <p>Tests from nested classes have test case IDs containing the {@code $} separator and should
   * be attached to their respective nested class nodes, not the root.
   *
   * @param tests all tests from root node
   * @return filtered list of root class tests only
   */
  private List<QAPTest> filterRootClassTests(List<QAPTest> tests) {
    if (tests == null) {
      return new ArrayList<>();
    }
    return tests.stream()
        .filter(this::isRootClassTest)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * Builds a map of parent class key to list of child classes.
   *
   * <p>Uses the {@code $} separator in class keys to determine parent-child relationships. Classes
   * without valid parent keys (orphans) are ignored - these shouldn't exist in normal execution but
   * could occur if store data is corrupted.
   *
   * @param nestedClasses all nested class nodes (excluding root)
   * @return map of parent key to list of children
   */
  private Map<String, List<QAPTestClass>> buildChildrenMap(
      Map<String, QAPTestClass> nestedClasses) {
    Map<String, List<QAPTestClass>> childrenMap = new HashMap<>();

    for (Map.Entry<String, QAPTestClass> entry : nestedClasses.entrySet()) {
      String classKey = entry.getKey();
      String parentClassKey = getParentClassKey(classKey);

      if (parentClassKey == null) {
        continue;
      }

      childrenMap.computeIfAbsent(parentClassKey, k -> new ArrayList<>()).add(entry.getValue());
    }

    return childrenMap;
  }

  /**
   * Recursively attaches child classes to their parent nodes.
   *
   * <p>Traverses the class hierarchy tree and sets the children list for each node. Includes depth
   * limit to prevent stack overflow in case of corrupted data (circular references shouldn't occur
   * with normal JUnit test classes but defensive programming prevents crashes).
   *
   * @param node the current node to process
   * @param childrenMap map of parent keys to children lists
   * @param depth current recursion depth (for safety limit)
   */
  private void attachChildrenRecursively(
      QAPTestClass node, Map<String, List<QAPTestClass>> childrenMap, int depth) {
    final int MAX_DEPTH = 10;
    if (depth >= MAX_DEPTH) {
      return;
    }

    String classKey = node.getClassKey();
    List<QAPTestClass> children = childrenMap.get(classKey);

    if (children != null && !children.isEmpty()) {
      node.setChildren(children);

      for (QAPTestClass child : children) {
        attachChildrenRecursively(child, childrenMap, depth + 1);
      }
    }
  }

  /**
   * Extracts the parent class key from a nested class key by removing the last nested segment.
   *
   * <p>Example: {@code "com.example.Outer$Inner" → "com.example.Outer"}
   *
   * @param classKey the full class key
   * @return parent class key, or null if this is not a nested class
   */
  private String getParentClassKey(String classKey) {
    if (classKey == null) {
      return null;
    }
    int idx = classKey.lastIndexOf(NESTED_CLASS_SEPARATOR);
    if (idx <= 0) {
      return null;
    }
    return classKey.substring(0, idx);
  }

  /**
   * Determines if a test belongs to the root class (not a nested class).
   *
   * <p>Tests from nested classes have test case IDs containing the {@code $} separator.
   *
   * @param test the test to check
   * @return true if this is a root class test, false if from nested class
   */
  private boolean isRootClassTest(QAPTest test) {
    if (test == null || test.getTestCaseId() == null) {
      return false;
    }
    return !test.getTestCaseId().contains(NESTED_CLASS_SEPARATOR);
  }
}
