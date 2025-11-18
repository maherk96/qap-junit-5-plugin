package com.mk.fx.qa.qap.junit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

class TagExtractorTest {

  @Tag("TopTag")
  static class Top {}

  @Tag("MidTag")
  static class Mid extends Top {}

  @Tag("LeafTag")
  static class Leaf extends Mid {
    @Tag("MethodTag")
    void taggedMethod() {}

    void untaggedMethod() {}
  }

  @Test
  void methodTags_returns_only_method_level_tags() throws Exception {
    Method m = Leaf.class.getDeclaredMethod("taggedMethod");

    ExtensionContext ctx = Mockito.mock(ExtensionContext.class);
    when(ctx.getTestMethod()).thenReturn(Optional.of(m));

    var tags = TagExtractor.methodTags(ctx);
    assertEquals(1, tags.size());
    assertTrue(tags.contains("MethodTag"));
  }

  @Test
  void class_and_inherited_tags_are_separated() {
    ExtensionContext root = Mockito.mock(ExtensionContext.class);
    when(root.getParent()).thenReturn(Optional.empty());

    ExtensionContext top = Mockito.mock(ExtensionContext.class);
    when(top.getParent()).thenReturn(Optional.of(root));
    when(top.getTestClass()).thenReturn(Optional.of(Top.class));

    ExtensionContext mid = Mockito.mock(ExtensionContext.class);
    when(mid.getParent()).thenReturn(Optional.of(top));
    when(mid.getTestClass()).thenReturn(Optional.of(Mid.class));

    ExtensionContext leafCtx = Mockito.mock(ExtensionContext.class);
    when(leafCtx.getParent()).thenReturn(Optional.of(mid));
    when(leafCtx.getTestClass()).thenReturn(Optional.of(Leaf.class));

    var currentTags = TagExtractor.classTags(leafCtx);
    assertEquals(1, currentTags.size());
    assertTrue(currentTags.contains("LeafTag"));

    var inherited = TagExtractor.inheritedClassTags(leafCtx);
    assertEquals(2, inherited.size());
    assertTrue(inherited.contains("TopTag"));
    assertTrue(inherited.contains("MidTag"));
  }
}
