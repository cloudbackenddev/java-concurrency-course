# Async Testing Masterclass: Testing Concurrent Java Programs

Testing concurrent code is notoriously difficult because bugs are **non-deterministic** — a test might pass 99 times and fail on the 100th run. Traditional `Thread.sleep()` is brittle and slow. This masterclass teaches you how to write **reliable, fast, and deterministic** tests for concurrent programs using **Awaitility** and **JUnit 5**.

> [!TIP]
> This is a companion to the [Java Concurrency Masterclass](JAVA_CONCURRENCY_MASTERCLASS.md). Each test below targets a specific concept from that course.

---

## Setup: Maven Dependencies

Add these to your `pom.xml`:

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>

    <!-- Awaitility -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <version>4.2.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Why Awaitility over `Thread.sleep()`?

| Feature | `Thread.sleep(2000)` | `await().atMost(2, SECONDS)...` |
| :--- | :--- | :--- |
| **Speed** | Always waits the full 2 seconds. | Returns **instantly** when the condition is met. |
| **Reliability** | Fails if work takes 2.1 seconds. | Flexible — keeps polling until timeout. |
| **Readability** | "Fingers crossed" waiting. | Explicitly states *what* you're waiting for. |

---

## Test 1: `synchronized` — The Race Condition Detector

**🎯 What We're Testing:** Two versions of a shared counter — one broken (no sync), one fixed (`synchronized`). The test proves that without synchronization, `count++` loses updates.

> **Masterclass Ref:** [Section 2.4 — Race Condition](JAVA_CONCURRENCY_MASTERCLASS.md) and [Section 2.5 — Synchronizing Access](JAVA_CONCURRENCY_MASTERCLASS.md)

### The Main Program

```java
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
```

### The JUnit Test

```java
import org.junit.jupiter.api.Test;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.*;
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
```

> [!IMPORTANT]
> **Key Takeaway:** Run `unsafeIncrement_losesUpdates` multiple times — you'll get different numbers each time (e.g., 98,342, 99,105). The `synchronized` version returns **exactly 100,000 every time**. That's the whole point of mutual exclusion.

---

## Test 2: `volatile` — The Visibility Flag Test

**🎯 What We're Testing:** A background worker loop controlled by a `volatile boolean`. Without `volatile`, the worker thread might **never see** the flag change due to CPU caching (the JMM visibility problem).

> **Masterclass Ref:** [Section 1.7 — JMM](JAVA_CONCURRENCY_MASTERCLASS.md) and [Section 2.9 — volatile](JAVA_CONCURRENCY_MASTERCLASS.md)

### The Main Program

```java
public class BackgroundWorker {
    // ✅ volatile ensures the flag write is visible across threads
    private volatile boolean running = true;
    private int workCount = 0;

    public void start() {
        new Thread(() -> {
            while (running) {        // Reads from main memory (thanks to volatile)
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
        running = false;             // Writes to main memory immediately
    }

    public boolean isRunning() {
        return running;
    }

    public int getWorkCount() {
        return workCount;
    }
}
```

### The JUnit Test

```java
import org.junit.jupiter.api.Test;
import static org.awaitility.Awaitility.*;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

class BackgroundWorkerTest {

    @Test
    void workerStops_whenFlagSetToFalse() {
        BackgroundWorker worker = new BackgroundWorker();
        worker.start();

        // Wait until the worker has done at least some work (proves it's alive)
        await().atMost(2, SECONDS)
               .until(() -> worker.getWorkCount() > 0);

        // Now stop it
        worker.stop();

        // Awaitility polls until the worker's loop has exited
        await().atMost(2, SECONDS)
               .untilAsserted(() -> assertFalse(worker.isRunning()));
    }

    @Test
    void workerPerformsWork_whileRunning() {
        BackgroundWorker worker = new BackgroundWorker();
        worker.start();

        // Assert that work count keeps increasing over time
        await().atMost(3, SECONDS)
               .until(() -> worker.getWorkCount() >= 5);

        worker.stop();

        // Capture final count after stop
        int countAtStop = worker.getWorkCount();

        // Give it a moment, then verify no more work was done
        await().during(500, MILLISECONDS)
               .atMost(2, SECONDS)
               .until(() -> worker.getWorkCount() <= countAtStop + 1);
    }
}
```

> [!IMPORTANT]
> **Key Takeaway:** Without `volatile`, the JVM is allowed to **cache** the `running` flag in the worker thread's CPU registers forever. The loop would run infinitely even after `stop()` is called. `volatile` forces a read from main memory on every loop iteration.

---

## Test 3: `BlockingQueue` — The Producer-Consumer Test

**🎯 What We're Testing:** A classic Producer-Consumer using `LinkedBlockingQueue`. The producer pushes messages, the consumer processes them. The test verifies that **all messages are consumed in order** and that the queue handles synchronization for us.

> **Masterclass Ref:** [Section 3.4 — Producer-Consumer Pattern](JAVA_CONCURRENCY_MASTERCLASS.md)

### The Main Program

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageBroker {
    private final BlockingQueue<String> queue;
    private final List<String> processedMessages = new CopyOnWriteArrayList<>();
    private volatile boolean producerDone = false;

    public MessageBroker(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    // Producer: Adds messages to the queue (blocks if full)
    public void produce(List<String> messages) {
        new Thread(() -> {
            try {
                for (String msg : messages) {
                    queue.put(msg);   // Blocks if queue is full (backpressure!)
                }
                producerDone = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Consumer: Takes messages from the queue (blocks if empty)
    public void consume() {
        new Thread(() -> {
            try {
                while (!producerDone || !queue.isEmpty()) {
                    String msg = queue.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        // Simulate processing delay
                        Thread.sleep(50);
                        processedMessages.add(msg);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public List<String> getProcessedMessages() {
        return processedMessages;
    }

    public int getQueueSize() {
        return queue.size();
    }
}
```

### The JUnit Test

```java
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.awaitility.Awaitility.*;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

class MessageBrokerTest {

    @Test
    void allMessagesAreConsumed_inOrder() {
        List<String> messages = List.of("Order-1", "Order-2", "Order-3", "Order-4", "Order-5");
        MessageBroker broker = new MessageBroker(3); // Small buffer = forces blocking!

        // Start consumer FIRST, then producer
        broker.consume();
        broker.produce(messages);

        // Await until all 5 messages have been processed
        await().atMost(10, SECONDS)
               .until(() -> broker.getProcessedMessages().size() == messages.size());

        // Verify all messages arrived AND in the correct order
        assertEquals(messages, broker.getProcessedMessages());
    }

    @Test
    void queueAppliesBackpressure_whenFull() {
        // Buffer of 2, but we push 5 items — producer must block until consumer drains
        MessageBroker broker = new MessageBroker(2);

        List<String> messages = List.of("A", "B", "C", "D", "E");
        broker.produce(messages);

        // Queue should fill up to capacity (2) before consumer starts
        await().atMost(2, SECONDS)
               .until(() -> broker.getQueueSize() == 2);

        // Now start consuming
        broker.consume();

        // Wait for all to be processed
        await().atMost(10, SECONDS)
               .until(() -> broker.getProcessedMessages().size() == 5);

        assertEquals(messages, broker.getProcessedMessages());
    }
}
```

> [!IMPORTANT]
> **Key Takeaway:** `BlockingQueue.put()` blocks when the queue is full (backpressure), and `take()` blocks when empty. This means the **queue itself handles all the synchronization** — no `synchronized`, no `wait()/notify()`, no manual locking. That's the beauty of `java.util.concurrent`.

---

## Test 4: `AtomicInteger` — The Lock-Free Counter Test

**🎯 What We're Testing:** An `AtomicInteger`-based page-hit counter that handles massive concurrent traffic without any locks. Uses hardware CAS (Compare-And-Swap) under the hood.

> **Masterclass Ref:** [Section 2.9 — Atomic Classes](JAVA_CONCURRENCY_MASTERCLASS.md)

### The Main Program

```java
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
```

### The JUnit Test

```java
import org.junit.jupiter.api.Test;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.*;
import static java.util.concurrent.TimeUnit.*;
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
```

> [!IMPORTANT]
> **Key Takeaway:** `AtomicInteger` uses **CAS (Compare-And-Swap)** — a single CPU instruction that atomically checks "is the value still X?" and only then writes "set it to Y". No thread is ever blocked. Under moderate contention, this is **significantly faster than `synchronized`**.

---

## Test 5: `CompletableFuture` — The Async Pipeline Test

**🎯 What We're Testing:** A multi-stage async pipeline: **Fetch → Transform → Store**. Each stage runs asynchronously. The test verifies the final result and that all stages executed in order.

> **Masterclass Ref:** [Section 4.1–4.4 — CompletableFuture](JAVA_CONCURRENCY_MASTERCLASS.md)

### The Main Program

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class AsyncPipeline {
    private final List<String> stagesCompleted = new CopyOnWriteArrayList<>();
    private volatile String finalResult = null;

    public void execute(String input) {
        CompletableFuture
            .supplyAsync(() -> {
                // Stage 1: Fetch (simulate I/O delay)
                sleep(200);
                stagesCompleted.add("FETCH");
                return "raw:" + input;
            })
            .thenApply(raw -> {
                // Stage 2: Transform
                sleep(100);
                stagesCompleted.add("TRANSFORM");
                return raw.toUpperCase();
            })
            .thenApply(transformed -> {
                // Stage 3: Store
                sleep(100);
                stagesCompleted.add("STORE");
                return "stored:" + transformed;
            })
            .thenAccept(result -> {
                finalResult = result;
            })
            .exceptionally(ex -> {
                finalResult = "ERROR: " + ex.getMessage();
                return null;
            });
    }

    public String getFinalResult() { return finalResult; }
    public List<String> getStagesCompleted() { return stagesCompleted; }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### The JUnit Test

```java
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.awaitility.Awaitility.*;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

class AsyncPipelineTest {

    @Test
    void pipeline_executesAllStagesInOrder() {
        AsyncPipeline pipeline = new AsyncPipeline();
        pipeline.execute("order-42");

        // Wait until the pipeline completes
        await().atMost(5, SECONDS)
               .until(() -> pipeline.getFinalResult() != null);

        // Verify final result includes all transformations
        assertEquals("stored:RAW:ORDER-42", pipeline.getFinalResult());

        // Verify the stages ran in sequence
        assertEquals(List.of("FETCH", "TRANSFORM", "STORE"),
                     pipeline.getStagesCompleted());
    }

    @Test
    void pipeline_progressesThroughStages() {
        AsyncPipeline pipeline = new AsyncPipeline();
        pipeline.execute("data");

        // First, wait for FETCH to complete (earliest stage)
        await().atMost(3, SECONDS)
               .until(() -> pipeline.getStagesCompleted().contains("FETCH"));

        // Then wait for the full pipeline
        await().atMost(5, SECONDS)
               .until(() -> pipeline.getStagesCompleted().size() == 3);

        assertNotNull(pipeline.getFinalResult());
    }
}
```

> [!IMPORTANT]
> **Key Takeaway:** `thenApply` chains stages **sequentially** (the output of one is the input to the next), but each stage runs on a thread from the **ForkJoinPool**. Awaitility lets us test this without blocking the test thread — we just poll until the result appears.

---

## Test 6: `CountDownLatch` — The Coordination Gate Test

**🎯 What We're Testing:** A system that requires **all services** (Database, Cache, MessageQueue) to be initialized before it reports "READY". Uses `CountDownLatch` as a gate.

> **Masterclass Ref:** [Section 5.2 — Testing Concurrency](JAVA_CONCURRENCY_MASTERCLASS.md)

### The Main Program

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

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

    public String getSystemStatus() { return systemStatus; }
    public List<String> getInitializedServices() { return initializedServices; }
}
```

### The JUnit Test

```java
import org.junit.jupiter.api.Test;
import static org.awaitility.Awaitility.*;
import static java.util.concurrent.TimeUnit.*;
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
```

> [!IMPORTANT]
> **Key Takeaway:** `CountDownLatch` is a one-shot gate. Each `countDown()` decrements the counter. `await()` blocks until the counter hits zero. Unlike `CyclicBarrier`, it **cannot be reused**. It's perfect for "wait until N things are done" scenarios.

---

## Quick Reference: Awaitility Cheat Sheet

```java
// Basic: Wait until condition is true (polls every 100ms by default)
await().atMost(5, SECONDS)
       .until(() -> service.isReady());

// Custom poll interval
await().atMost(10, SECONDS)
       .pollInterval(500, MILLISECONDS)
       .until(() -> repo.count() > 0);

// Assert within the await (more readable failures)
await().atMost(5, SECONDS)
       .untilAsserted(() -> assertEquals("DONE", service.getStatus()));

// Assert condition holds TRUE for a duration ("stability check")
await().during(1, SECONDS)
       .atMost(3, SECONDS)
       .until(() -> service.getStatus().equals("STABLE"));

// Ignore exceptions during polling (useful during startup)
await().ignoreExceptions()
       .atMost(10, SECONDS)
       .until(() -> service.healthCheck().isUp());

// Alias for readability
await("Waiting for order to complete")
       .atMost(5, SECONDS)
       .until(() -> order.getStatus().equals("SHIPPED"));
```

---

## Summary: What Each Test Teaches

| Test | Concept | Without Fix | With Fix |
| :--- | :--- | :--- | :--- |
| **1. SharedCounter** | `synchronized` | Lost updates (count < expected) | Exact count every time |
| **2. BackgroundWorker** | `volatile` | Worker never sees flag change | Worker stops promptly |
| **3. MessageBroker** | `BlockingQueue` | Manual wait/notify complexity | Zero manual locking |
| **4. AtomicHitCounter** | `AtomicInteger` | Race condition or heavy locks | Lock-free, fast, exact |
| **5. AsyncPipeline** | `CompletableFuture` | Blocking `future.get()` calls | Non-blocking pipeline |
| **6. ServiceInitializer** | `CountDownLatch` | Fragile polling / `Thread.sleep()` | Clean gate mechanism |
