package com.mk.fx.qa.qap.junit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mk.fx.qa.qap.junit.annotation.CoverageItem;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

class CoverageItemExtractorTest {

  static class SimpleMethods {
    @CoverageItem("METHOD-400")
    void singleItem() {}

    @CoverageItem("M1")
    @CoverageItem("M2")
    @CoverageItem("M3")
    void multipleItems() {}

    void untaggedMethod() {}
  }

  @Test
  void methodCoverageItems_returns_single_item() throws Exception {
    Method m = SimpleMethods.class.getDeclaredMethod("singleItem");

    ExtensionContext ctx = Mockito.mock(ExtensionContext.class);
    when(ctx.getTestMethod()).thenReturn(Optional.of(m));

    List<String> items = CoverageItemExtractor.methodCoverageItems(ctx);
    assertEquals(1, items.size());
    assertEquals("METHOD-400", items.get(0));
  }

  @Test
  void methodCoverageItems_returns_multiple_items_in_order() throws Exception {
    Method m = SimpleMethods.class.getDeclaredMethod("multipleItems");

    ExtensionContext ctx = Mockito.mock(ExtensionContext.class);
    when(ctx.getTestMethod()).thenReturn(Optional.of(m));

    List<String> items = CoverageItemExtractor.methodCoverageItems(ctx);
    assertEquals(3, items.size());
    assertTrue(items.contains("M1"));
    assertTrue(items.contains("M2"));
    assertTrue(items.contains("M3"));
  }

  @Test
  void methodCoverageItems_returns_empty_when_no_annotations() throws Exception {
    Method m = SimpleMethods.class.getDeclaredMethod("untaggedMethod");

    ExtensionContext ctx = Mockito.mock(ExtensionContext.class);
    when(ctx.getTestMethod()).thenReturn(Optional.of(m));

    List<String> items = CoverageItemExtractor.methodCoverageItems(ctx);
    assertNotNull(items);
    assertTrue(items.isEmpty());
  }

  @Test
  void methodCoverageItems_returns_empty_when_no_method_present() {
    ExtensionContext ctx = Mockito.mock(ExtensionContext.class);
    when(ctx.getTestMethod()).thenReturn(Optional.empty());

    List<String> items = CoverageItemExtractor.methodCoverageItems(ctx);
    assertNotNull(items);
    assertTrue(items.isEmpty());
  }
}
