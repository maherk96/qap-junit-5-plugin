package com.mk.fx.qa.qap.junit.util;

import com.mk.fx.qa.qap.junit.annotation.CoverageItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extracts {@link CoverageItem} annotations from test methods.
 *
 * <p>Only extracts method-level coverage items. Multiple {@code @CoverageItem} annotations on the
 * same method are collected into a list.
 */
public final class CoverageItemExtractor {

  private CoverageItemExtractor() {}

  /**
   * Extracts coverage items declared directly on the test method.
   *
   * @param context the extension context
   * @return list of coverage item values, or empty list if none found
   */
  public static List<String> methodCoverageItems(ExtensionContext context) {
    return context
        .getTestMethod()
        .map(
            m -> {
              List<String> items = new ArrayList<>();
              for (CoverageItem c : m.getAnnotationsByType(CoverageItem.class)) {
                items.add(c.value());
              }
              return Collections.unmodifiableList(items);
            })
        .orElse(Collections.emptyList());
  }
}
