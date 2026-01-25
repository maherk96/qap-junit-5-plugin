package com.mk.fx.qa.qap.junit.extension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

class DisplayNameResolverTest {

  @DisplayName("PrettyClass")
  static class Clazz {
    @DisplayName("PrettyMethod")
    void m() {}

    void plain() {}
  }

  @Test
  void resolves_run_display_name_with_parameterized_like_names() throws Exception {
    DisplayNameResolver r = new DisplayNameResolver();
    ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
    Method m = Clazz.class.getDeclaredMethod("m");
    when(ctx.getTestMethod()).thenReturn(Optional.of(m));
    when(ctx.getRequiredTestMethod()).thenReturn(m);

    // Should preserve dynamic names that are not auto-generated
    assertEquals("Run 1 with value=A", r.resolveRunDisplayName(ctx, "m", "Run 1 with value=A"));

    // Falls back to @DisplayName when auto-generated
    assertEquals("PrettyMethod", r.resolveRunDisplayName(ctx, "m", "m()"));
  }

  @Test
  void resolves_method_and_class_display_names() throws Exception {
    DisplayNameResolver r = new DisplayNameResolver();
    ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);

    when(ctx.getTestClass()).thenReturn(Optional.of(Clazz.class));
    Method plain = Clazz.class.getDeclaredMethod("plain");
    when(ctx.getTestMethod()).thenReturn(Optional.of(plain));

    assertEquals("PrettyClass", r.resolveClassDisplayName(ctx));
    assertEquals("plain", r.resolveMethodDisplayName(ctx));
  }

  @DisplayName("Top")
  static class Top {}

  @DisplayName("Mid")
  static class Mid {}

  @DisplayName("Leaf")
  static class Leaf {}

  @Test
  void builds_parent_chain_excluding_current_class() {
    DisplayNameResolver r = new DisplayNameResolver();

    ExtensionContext leaf = mock(ExtensionContext.class);
    ExtensionContext mid = mock(ExtensionContext.class);
    ExtensionContext top = mock(ExtensionContext.class);

    when(leaf.getParent()).thenReturn(Optional.of(mid));
    when(mid.getParent()).thenReturn(Optional.of(top));
    when(top.getParent()).thenReturn(Optional.empty());

    when(leaf.getTestClass()).thenReturn(Optional.of(Leaf.class));
    when(mid.getTestClass()).thenReturn(Optional.of(Mid.class));
    when(top.getTestClass()).thenReturn(Optional.of(Top.class));

    List<String> chain = r.buildParentChain(leaf);
    assertEquals(List.of("Top", "Mid"), chain);
  }
}
