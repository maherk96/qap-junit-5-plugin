package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPJunitLifeCycleEvent;
import com.mk.fx.qa.qap.junit.store.StoreManager;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Map;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.TEST_CLASS_DATA_KEY;


public class QAPJunitLifeCycleEventCreator implements ILifeCycleEventCreator {
    private final Map<String, Throwable> failedInits;

    public QAPJunitLifeCycleEventCreator(Map<String, Throwable> failedInits) {
        this.failedInits = failedInits;
    }

    @Override
    public void createLifeCycleEvent(LifeCycleEvent event, ExtensionContext context) {
        var launch = StoreManager.getClassStoreData(context, TEST_CLASS_DATA_KEY, QAPJunitLaunch.class);

        QAPJunitLifeCycleEvent lifeCycleEvent = new QAPJunitLifeCycleEvent(null, null, null);

        // Optional.ofNullable(failedInits.get(context.getUniqueId()))
        //     .map(init -> new QAPJunitLifeCycleEvent(
        //             event,
        //             DataCompressor.compressData(init.getMessage()),
        //             testLogs))
        //     .orElseGet(() -> new QAPJunitLifeCycleEvent(event, null, testLogs));

        launch.getTestClass().getQapJunitLifeCycleEvent().add(lifeCycleEvent);
    }

}