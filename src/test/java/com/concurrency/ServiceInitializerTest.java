package com.concurrency;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;

class ServiceInitializerTest {

    @Test
    void systemBecomesReady_onlyAfterAllServicesInit() {
        ServiceInitializer init = new ServiceInitializer(3);

        // Start the system waiting thread
        init.waitForAllServices();

        // System should NOT be ready yet
        assertEquals("STARTING", init.getSystemStatus());

        // Register services one by one
        init.registerServiceReady("Database");
        init.registerServiceReady("Cache");

        // Still NOT ready (only 2 of 3)
        await().during(300, MILLISECONDS)
                .atMost(2, SECONDS)
                .until(() -> init.getSystemStatus().equals("STARTING"));

        // Third service comes online
        init.registerServiceReady("MessageQueue");

        // NOW it should become READY
        await().atMost(5, SECONDS)
                .until(() -> init.getSystemStatus().equals("READY"));

        // All 3 services must be tracked
        assertEquals(3, init.getInitializedServices().size());
        assertTrue(init.getInitializedServices().contains("Database"));
        assertTrue(init.getInitializedServices().contains("Cache"));
        assertTrue(init.getInitializedServices().contains("MessageQueue"));
    }

    @Test
    void systemStaysStarting_ifNotAllServicesReady() {
        ServiceInitializer init = new ServiceInitializer(3);
        init.waitForAllServices();

        // Only register 2 out of 3 required services
        init.registerServiceReady("Database");
        init.registerServiceReady("Cache");

        // System must stay in STARTING state because latch count is still 1
        await().during(1, SECONDS)
                .atMost(3, SECONDS)
                .until(() -> init.getSystemStatus().equals("STARTING"));
    }
}
