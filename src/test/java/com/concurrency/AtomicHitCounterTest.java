package com.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AtomicHitCounterTest {

    @Test
    void concurrentHits_areNeverLost() throws InterruptedException {
        AtomicHitCounter counter = new AtomicHitCounter();
        int threads = 50;
        int hitsPerThread = 10_000;
        int expected = threads * hitsPerThread; // 500,000

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                for (int j = 0; j < hitsPerThread; j++) {
                    counter.recordHit();
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        // AtomicInteger guarantees EXACTLY 500,000 — no lost updates, no locks
        assertEquals(expected, counter.getHits(),
                "AtomicInteger must never lose a single hit under heavy contention");
    }

    @Test
    void resetAndGet_isAtomic() throws InterruptedException {
        AtomicHitCounter counter = new AtomicHitCounter();
        ExecutorService pool = Executors.newFixedThreadPool(10);

        // Record 1000 hits
        for (int i = 0; i < 1000; i++) {
            pool.submit(counter::recordHit);
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // getAndSet(0) atomically returns old value and resets
        int snapshot = counter.resetAndGetHits();
        assertEquals(1000, snapshot);
        assertEquals(0, counter.getHits(), "Counter must be zero after reset");
    }
}
