package com.concurrency;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

public class ServiceInitializer {
    private final CountDownLatch latch;
    private final List<String> initializedServices = new CopyOnWriteArrayList<>();
    private volatile String systemStatus = "STARTING";

    public ServiceInitializer(int serviceCount) {
        this.latch = new CountDownLatch(serviceCount);
    }

    // Each service calls this when it's ready
    public void registerServiceReady(String serviceName) {
        new Thread(() -> {
            try {
                // Simulate different startup times
                Thread.sleep((long) (Math.random() * 500 + 100));
                initializedServices.add(serviceName);
                latch.countDown(); // Signal: "I'm ready!"
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // The main system thread waits for ALL services
    public void waitForAllServices() {
        new Thread(() -> {
            try {
                latch.await(); // Blocks until count reaches 0
                systemStatus = "READY";
            } catch (InterruptedException e) {
                systemStatus = "FAILED";
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public String getSystemStatus() {
        return systemStatus;
    }

    public List<String> getInitializedServices() {
        return initializedServices;
    }
}
