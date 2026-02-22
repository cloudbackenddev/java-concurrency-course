package com.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicHitCounter {
    private final AtomicInteger hits = new AtomicInteger(0);

    // Lock-free, thread-safe increment using CAS
    public void recordHit() {
        hits.incrementAndGet();
    }

    public int getHits() {
        return hits.get();
    }

    // Atomically reset and return old value
    public int resetAndGetHits() {
        return hits.getAndSet(0);
    }
}
