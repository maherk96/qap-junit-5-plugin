package com.mk.fx.qa.qap.junit.core;

import java.util.UUID;

public class QAPLaunchIdGenerator {

    private static final String SYSTEM_PROPERTY_LAUNCH_ID = "launchID";
    private static final int MAX_LAUNCH_ID_LENGTH = 50;
    private static final int UUID_LENGTH = 12;

    public void generateLaunchId() {
        String current = System.getProperty(SYSTEM_PROPERTY_LAUNCH_ID);

        if (isFullLaunchId(current)) {
            return;
        }

        String base = extractBase(current);
        String launchId = base + "-" + generateShortUUID();
        System.setProperty(SYSTEM_PROPERTY_LAUNCH_ID, truncate(launchId));
    }

    public String getLaunchId() {
        return System.getProperty(SYSTEM_PROPERTY_LAUNCH_ID);
    }

    /**
     * Generates a launch id only if not already present, with a simple synchronization to
     * avoid check-then-act races across threads within the same JVM.
     */
    public synchronized void generateIfAbsent() {
        String current = getLaunchId();
        if (!isFullLaunchId(current)) {
            generateLaunchId();
        }
    }

    private boolean isFullLaunchId(String value) {
        return value != null && value.matches(".+[-a-zA-Z0-9]{" + UUID_LENGTH + ",}");
    }

    /** Extracts the base from the existing property or uses a default if empty. */
    private String extractBase(String current) {
        if (current == null || current.isBlank()) {
            return "TestLaunch";
        }
        return current.split("-")[0];
    }

    /** Generates a short random UUID string with no dashes. */
    private String generateShortUUID() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, UUID_LENGTH);
    }

    /** Truncates the launch ID to the max length, removing trailing dashes if needed. */
    private String truncate(String launchId) {
        if (launchId.length() > MAX_LAUNCH_ID_LENGTH) {
            return launchId.substring(0, MAX_LAUNCH_ID_LENGTH).replaceAll("-+$", "");
        }
        return launchId;
    }
}
