package com.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SharedCounterTest {

    private static final int THREADS = 10;
    private static final int INCREMENTS_PER_THREAD = 10_000;
    private static final int EXPECTED = THREADS * INCREMENTS_PER_THREAD; // 100,000

    @Test
    void unsafeIncrement_losesUpdates() throws InterruptedException {
        SharedCounter counter = new SharedCounter();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    counter.unsafeIncrement();
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // The count will almost certainly be LESS than 100,000 due to race conditions
        System.out.println("Unsafe count: " + counter.getCount() + " (expected " + EXPECTED + ")");
        assertTrue(counter.getCount() <= EXPECTED,
                "Count should not exceed expected (may be less due to lost updates)");
    }

    @Test
    void safeIncrement_neverLosesUpdates() throws InterruptedException {
        SharedCounter counter = new SharedCounter();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    counter.safeIncrement();
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // synchronized guarantees exactly 100,000 every single time
        assertEquals(EXPECTED, counter.getCount(),
                "Synchronized increment must never lose updates");
    }
}
