# Virtual Threads Masterclass (Java 21+)

Virtual threads are a groundbreaking feature introduced in Java 21 (Project Loom) designed to make high-concurrency applications easier to write, maintain, and scale.

## 1. What are Virtual Threads?

Traditionally, Java threads (**Platform Threads**) were thin wrappers around operating system (OS) threads. OS threads are "heavy":
*   **Memory**: Each consumes ~1MB of stack space.
*   **Performance**: Context switching happens at the OS level and is expensive.
*   **Scalability**: You can typically only run a few thousand before system resources exhaust.

**Virtual Threads** decouple the Java thread from the OS thread:
*   **Lightweight**: They consume only a few KB of heap memory.
*   **JVM Managed**: The JVM handles scheduling, not the OS.
*   **Massive Scalability**: You can run **millions** of virtual threads on a single machine.

### The "One Thread per Request" Dream
Before virtual threads, developers had to use complex asynchronous programming (like `CompletableFuture` or Reactive libraries) to scale. Virtual threads bring back the simple "one thread per request" model without the memory overhead.

---

## 2. Platform vs. Virtual vs. CompletableFuture

| Feature | Platform Threads | Virtual Threads | CompletableFuture |
| :--- | :--- | :--- | :--- |
| **Managed by** | Operating System | JVM (Java Runtime) | JVM (Pool based) |
| **Resource Cost** | High (~1MB/thread) | Very Low (KB range) | Low (Task based) |
| **Context Switch** | Expensive (Kernel) | Cheap (User-space) | Non-blocking |
| **Complexity** | Simple (Imperative) | Simple (Imperative) | Complex (Callback/Fluent) |
| **Best For** | CPU-bound tasks | I/O-bound tasks | Async orchestration |

---

## 3. How to Use Virtual Threads

In most cases, you don't create virtual threads manually. Instead, use an `ExecutorService`.

### Using `newVirtualThreadPerTaskExecutor`
This executor creates a new virtual thread for every task you submit.

```java
import java.util.concurrent.Executors;

public class VirtualThreadDemo {
    public static void main(String[] args) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10_000; i++) {
                int taskId = i;
                executor.submit(() -> {
                    System.out.println("Task " + taskId + " on " + Thread.currentThread());
                    // Simulate I/O (this blocks the Virtual Thread, but NOT the OS thread)
                    Thread.sleep(100); 
                    return taskId;
                });
            }
        } // Executor auto-closes and waits for tasks
    }
}
```

---

## 4. Production Lessons & Pitfalls

Using virtual threads isn't "magic." There are critical production lessons to keep in mind.

### 4.1. Avoid CPU-Bound Tasks
Virtual threads are optimized for **I/O-heavy** tasks (database calls, API requests). For CPU-intensive work (calculating primes, image processing), use a standard `FixedThreadPool` sized to the number of CPU cores.

### 4.2. Pining Carrier Threads
A virtual thread runs on top of a platform thread (called a **Carrier Thread**). Some operations can "pin" the virtual thread to its carrier, preventing other virtual threads from running:
*   **`synchronized` blocks/methods**: Replace with `ReentrantLock` where possible.
*   **Native Methods**: JNI calls.
*   **`Object.wait()`**: Use `java.util.concurrent` locks instead.

> [!TIP]
> Use the JVM flag `-Djdk.tracePinnedThreads=full` to detect where pinning occurs in your logs.

### 4.3. Heap Memory Management
While platform threads use native memory, virtual threads use the **JVM Heap**. 
*   **Memory Pressure**: Millions of virtual threads can quickly fill your heap if their stacks grow large.
*   **Adjustment**: You may need to increase your `-Xmx` (Max Heap) when switching to virtual threads.

### 4.4. Don't Pool Virtual Threads
Virtual threads are disposable. **Do not use a `FixedThreadPool` of virtual threads.** Creating them is nearly "free." Just use `newVirtualThreadPerTaskExecutor`.

### 4.5. Rate Limiting with Semaphores
Because you can now spawn millions of threads, you might accidentally DDOS your own database or external APIs. 
**Old way**: Limit concurrency by pooling threads. 
**New way**: Use a `Semaphore` to limit access to a specific resource.

```java
private static final Semaphore DB_LIMITER = new Semaphore(50);

public void callDatabase() {
    try {
        DB_LIMITER.acquire();
        // perform work
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        DB_LIMITER.release();
    }
}
```

---

## 5. ThreadLocal vs. Scoped Values

Virtual threads are short-lived. Traditional `ThreadLocal` for **caching** expensive objects (like `SimpleDateFormat`) is useless because the thread dies after one task.
*   For **Context Sharing** (Security context, Transaction IDs): Use `ThreadLocal` but remember to `remove()`.
*   Modern Alternative: Explore **Scoped Values** (Java 21 Preview) for better performance and safety.

---

## 6. Monitoring and Debugging

Virtual threads do not appear in traditional thread dumps (like `jstack`) because there are too many of them.

### Thread Dumps
Use `jcmd` to generate a modern thread dump that includes virtual threads:
```bash
jcmd <PID> Thread.dump_to_file -format=json threads.json
```

### JDK Flight Recorder (JFR)
JFR includes specific events for virtual threads:
*   `jdk.VirtualThreadStart` and `jdk.VirtualThreadEnd`
*   `jdk.VirtualThreadPinned` (Crucial for finding performance bottlenecks!)

---

## Summary: When to Switch?
1.  **Switch if**: You have a high-concurrency web app with lots of I/O (blocking).
2.  **Stay with Platform Threads if**: Your app is low-concurrency or purely CPU-bound.
3.  **Refactor if**: Your code relies heavily on `synchronized` or `ThreadLocal` for caching.

---

## Appendix: Runnable Examples

### Example 1: Virtual Threads vs. CompletableFuture Benchmark
This example compares the execution time of 10,000 tasks that simulate a 10ms I/O delay.

```java
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;

public class VirtualThreadBenchmark {
    public static void main(String[] args) {
        long startTime, endTime;
        int taskCount = 10_000;
        int sleepTimeMillis = 10;
        
        System.out.println("Starting Benchmark with " + taskCount + " tasks...");

        // 1. Virtual Threads Benchmark
        startTime = System.currentTimeMillis();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try { 
                        Thread.sleep(sleepTimeMillis); 
                    } catch (InterruptedException e) { 
                        Thread.currentThread().interrupt(); 
                    }
                });
            }
        } // Auto-closes and waits for completion
        endTime = System.currentTimeMillis();
        System.out.println("Virtual Threads Execution Time: " + (endTime - startTime) + " ms");
        
        // 2. CompletableFuture Benchmark
        startTime = System.currentTimeMillis();
        CompletableFuture<?>[] futures = new CompletableFuture<?>[taskCount];
        for (int i = 0; i < taskCount; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try { 
                    Thread.sleep(sleepTimeMillis); 
                } catch (InterruptedException e) { 
                    Thread.currentThread().interrupt(); 
                }
            });
        }
        CompletableFuture.allOf(futures).join();
        endTime = System.currentTimeMillis();
        System.out.println("CompletableFuture (ForkJoinPool) Execution Time: " + (endTime - startTime) + " ms");
    }
}
```

### Example 2: Rate Limiting with Semaphores
This example demonstrates how to use a `Semaphore` to prevent overwhelming an external resource when using virtual threads.

```java
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreRateLimiter {
    // Limit to 5 concurrent "expensive" calls
    private static final Semaphore LIMITER = new Semaphore(5);

    public static void main(String[] args) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= 20; i++) {
                int taskId = i;
                executor.submit(() -> {
                    try {
                        if (LIMITER.tryAcquire(1, TimeUnit.SECONDS)) {
                            try {
                                System.out.println("Task " + taskId + " is accessing the resource...");
                                Thread.sleep(500); // Simulate work
                            } finally {
                                LIMITER.release();
                                System.out.println("Task " + taskId + " finished.");
                            }
                        } else {
                            System.err.println("Task " + taskId + " timed out waiting for resource.");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
    }
}
```
