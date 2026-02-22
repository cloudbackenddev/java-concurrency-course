package com.concurrency;

public class SharedCounter {
    private int count = 0;

    // ❌ BROKEN: Not thread-safe. count++ is NOT atomic (Read → Add → Write)
    public void unsafeIncrement() {
        count++;
    }

    // ✅ FIXED: synchronized ensures mutual exclusion
    public synchronized void safeIncrement() {
        count++;
    }

    public int getCount() {
        return count;
    }

    public void reset() {
        count = 0;
    }
}
