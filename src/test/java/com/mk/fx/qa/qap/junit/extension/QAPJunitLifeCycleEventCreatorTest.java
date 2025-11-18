package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.extension.support.InMemoryStore;
import com.mk.fx.qa.qap.junit.model.QAPHeader;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPTestClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.TEST_CLASS_DATA_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QAPJunitLifeCycleEventCreatorTest {

    @Test
    void adds_lifecycle_event_to_launch() {
        // Arrange
        ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getRequiredTestClass()).thenReturn((Class) QAPJunitLifeCycleEventCreatorTest.class);
        ExtensionContext root = mock(ExtensionContext.class);
        when(ctx.getRoot()).thenReturn(root);
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        QAPJunitLaunch launch = new QAPJunitLaunch(
                new QAPHeader(Instant.now().toEpochMilli(), "id"),
                java.util.List.of(new QAPTestClass("C", "C", java.util.Set.of()))
        );
        store.put(TEST_CLASS_DATA_KEY, launch);


        // Assert
        assertEquals(1, launch.getTestClasses().get(0).getQapJunitLifeCycleEvent().size());
    }
}
