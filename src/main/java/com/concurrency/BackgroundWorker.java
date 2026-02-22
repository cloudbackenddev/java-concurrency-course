package com.concurrency;

public class BackgroundWorker {
    // ✅ volatile ensures the flag write is visible across threads
    private volatile boolean running = true;
    private int workCount = 0;

    public void start() {
        new Thread(() -> {
            while (running) { // Reads from main memory (thanks to volatile)
                workCount++;
                try {
                    Thread.sleep(50); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    public void stop() {
        running = false; // Writes to main memory immediately
    }

    public boolean isRunning() {
        return running;
    }

    public int getWorkCount() {
        return workCount;
    }
}
