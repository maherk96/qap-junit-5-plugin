package com.mk.fx.qa.qap.junit.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Simple example showing @CoverageItem usage and expected JSON output.
 *
 * <p>Run this test and check the JSON output to see how coverage items are captured.
 */
@ExtendWith(QAPJunitExtension.class)
@Tag("demo")
@DisplayName("Coverage Item Demo")
class CoverageItemSimpleDemo {

  /**
   * Example test with single coverage item.
   *
   * <p>Expected JSON output:
   *
   * <pre>
   * {
   *   "methodName": "testWithSingleCoverageItem",
   *   "displayName": "Test with single coverage item",
   *   "coverageItems": ["REQ-100"],
   *   "tags": {
   *     "method": ["smoke"]
   *   }
   * }
   * </pre>
   */
  @Test
  @Tag("smoke")
  @CoverageItem("REQ-100")
  @DisplayName("Test with single coverage item")
  void testWithSingleCoverageItem() {
    assertEquals(2 + 2, 4);
  }

  /**
   * Example test with multiple coverage items.
   *
   * <p>Expected JSON output:
   *
   * <pre>
   * {
   *   "methodName": "testWithMultipleCoverageItems",
   *   "displayName": "Test covering multiple requirements",
   *   "coverageItems": ["REQ-200", "STORY-45", "BUG-789"],
   *   "tags": {
   *     "method": ["regression"]
   *   }
   * }
   * </pre>
   */
  @Test
  @Tag("regression")
  @CoverageItem("REQ-200")
  @CoverageItem("STORY-45")
  @CoverageItem("BUG-789")
  @DisplayName("Test covering multiple requirements")
  void testWithMultipleCoverageItems() {
    assertEquals("hello".length(), 5);
  }

  /**
   * Example test with no coverage items.
   *
   * <p>Expected JSON output:
   *
   * <pre>
   * {
   *   "methodName": "testWithNoCoverageItems",
   *   "displayName": "Test with no coverage tracking",
   *   "coverageItems": [],
   *   "tags": {
   *     "method": []
   *   }
   * }
   * </pre>
   */
  @Test
  @DisplayName("Test with no coverage tracking")
  void testWithNoCoverageItems() {
    assertEquals(true, true);
  }
}
