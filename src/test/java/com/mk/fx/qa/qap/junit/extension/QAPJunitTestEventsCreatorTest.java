package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QAPJunitTestEventsCreatorTest {

    @Tag("T1")
    static class WithClassTag { }

    @Test
    void startLaunchQAP_builds_launch_with_class_tags_only() {
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getRequiredTestClass()).thenReturn((Class) WithClassTag.class);
        when(ctx.getDisplayName()).thenReturn("WithClassTag");

        QAPJunitTestEventsCreator creator = new QAPJunitTestEventsCreator();

        QAPJunitLaunch launch = creator.startLaunchQAP(ctx);
        assertEquals("WithClassTag", launch.getTestClasses().get(0).getDisplayName());
        assertTrue(launch.getTestClasses().get(0).getTags().getClazz().contains("T1"));
    }
}
