# Java Concurrency Labs & Case Studies
*Interactive exercises to keep you awake!*

Welcome to the lab session. The best way to learn concurrency is to break things and then fix them. 

For each lab:
1.  **Run the "Broken" code.** Watch it fail.
2.  **Fix it** using the concepts from the Masterclass.
3.  **Check the Solution** only after you have tried!

---

## Lab 1: The Glitchy Inventory (Race Conditions)
**Concept:** Shared State & Atomicity

### The Scenario
You are running an exclusive flash sale for the new "Phone X". You verify stock (10 items) and then ship the item. 

### The Problem Code
Copy this into `Inventory.java`. Run it.

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Inventory {
    static int items = 10; // Shared resource

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(100);

        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> buy());
        }
        executor.shutdown();
    }

    static void buy() {
        if (items > 0) { // Check
            try { Thread.sleep(10); } catch (InterruptedException e) {} // Simulating processing time
            items--; // Act
            System.out.println("Item bought! Remaining: " + items);
        } else {
            // System.out.println("Out of stock!");
        }
    }
}
```

### The "Bug"
Run this code multiple times. 
**Observe:** You will likely see "Item bought! Remaining: -5" or even lower. You sold items you don't have!
**Why:** Multiple threads passed the `if (items > 0)` check *before* any of them decreased the count.

### Your Task
Fix the `buy()` method so that we **never** go below 0. 
*Hint:* You need to make the "Check-Then-Act" atomic.

<details>
<summary><b>Click to Reveal Solution</b></summary>

### Solution
We can use the `synchronized` keyword to ensure only one thread enters the check-and-decrement block at a time.

```java
static synchronized void buy() {
    if (items > 0) {
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        items--;
        System.out.println("Item bought! Remaining: " + items);
    } else {
        System.out.println("Out of stock!");
    }
}
```
**Alternative:** Use `AtomicInteger` (harder here because of the condition) or a `ReentrantLock`.
</details>

---

## Lab 2: The Stalled Intersection (Deadlocks)
**Concept:** Liveness & Lock Ordering

### The Scenario
Two trains (Train A and Train B) are approaching a crossing. 
- Train A is on **Track 1** and needs to cross **Track 2**.
- Train B is on **Track 2** and needs to cross **Track 1**.

### The Problem Code
Copy this into `Intersection.java`. Run it.

```java
public class Intersection {
    static final Object track1 = new Object();
    static final Object track2 = new Object();

    public static void main(String[] args) {
        // Train A
        new Thread(() -> {
            synchronized (track1) {
                System.out.println("Train A holds Track 1...");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                System.out.println("Train A waiting for Track 2...");
                
                synchronized (track2) {
                    System.out.println("Train A passing through!");
                }
            }
        }).start();

        // Train B
        new Thread(() -> {
            synchronized (track2) {
                System.out.println("Train B holds Track 2...");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                System.out.println("Train B waiting for Track 1...");
                
                synchronized (track1) {
                    System.out.println("Train B passing through!");
                }
            }
        }).start();
    }
}
```

### The "Bug"
Run the code.
**Observe:** The program hangs forever. Both threads are stuck "waiting for...".
**Why:** A "Cyclic Dependency". A holds 1 wants 2. B holds 2 wants 1.

### Your Task
Fix the deadlock so both trains can pass (one after another).
*Hint:* Change the order in which locks are acquired.

<details>
<summary><b>Click to Reveal Solution</b></summary>

### Solution
Ensure both threads acquire the locks in the **SAME ORDER**.

```java
// Train B (Fixed)
new Thread(() -> {
    // Both trains acquire Track 1 FIRST, then Track 2
    synchronized (track1) { // CHANGED ORDER
        System.out.println("Train B waiting for Track 2..."); // hold 1 now
        
        synchronized (track2) {
            System.out.println("Train B passing through!");
        }
    }
}).start();
```
Now, Train A grabs Track 1. Train B tries to grab Track 1 but waits. Train A grabs Track 2, finishes, and releases everything. Then Train B proceeds.
</details>

---

## Lab 3: The Slow Search Engine (Thread Pools)
**Concept:** Parallelism & ExecutorService

### The Scenario
You have a list of user names and need to "process" them (simulate a slow database lookup).

### The Problem Code
```java
import java.util.Arrays;
import java.util.List;

public class SearchEngine {
    public static void main(String[] args) {
        List<String> users = Arrays.asList("Alice", "Bob", "Charlie", "Dave", "Eve", "Frank");
        long start = System.currentTimeMillis();

        for (String user : users) {
             processUser(user);
        }

        long end = System.currentTimeMillis();
        System.out.println("Total time: " + (end - start) + "ms");
    }

    static void processUser(String user) {
        try {
            Thread.sleep(1000); // Simulate 1 second DB lookup
            System.out.println("Processed: " + user);
        } catch (InterruptedException e) {}
    }
}
```

### The "Bug"
**Observe:** It takes **6 seconds** to process 6 users. This is too slow!
**Why:** Single-threaded execution handles one user at a time.

### Your Task
Use an `ExecutorService` to process them in **Parallel**. Try to get the total time down to ~1 second.

<details>
<summary><b>Click to Reveal Solution</b></summary>

### Solution
Use a CachedThreadPool or FixedThreadPool.

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParallelSearch {
    public static void main(String[] args) throws InterruptedException {
        List<String> users = Arrays.asList("Alice", "Bob", "Charlie", "Dave", "Eve", "Frank");
        ExecutorService executor = Executors.newCachedThreadPool(); // Creates threads as needed
        
        long start = System.currentTimeMillis();

        for (String user : users) {
             executor.submit(() -> processUser(user));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS); // Wait for all to finish

        long end = System.currentTimeMillis();
        System.out.println("Total time: " + (end - start) + "ms");
    }
    // processUser method remains the same
}
```
**Result:** It should run in roughly 1005ms!
</details>

---

## Lab 4: The Order Processor (CompletableFuture)
**Concept:** Asynchronous Composition

### The Scenario
You need to process an order by:
1.  Fetching the User (500ms)
2.  Getting their Orders (Wait for Step 1, then 500ms)
3.  Calculating the Total (Wait for Step 2, then Instant)

### The Problem Code
The synchronous way blocks the thread for 1 second per request. 

```java
import java.util.concurrent.CompletableFuture;

public class OrderProcessor {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Standard blocking approach (Simulated)
        User user = getUser(1);
        Order order = getOrders(user);
        int total = calculateTotal(order);
        
        System.out.println("Total: $" + total);
        
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start) + "ms");
    }

    // ... Helper methods (getUser etc.) would be here
}
```

### Your Task
Rewrite this using `CompletableFuture`. 
- Fetch user asynchronously.
- **Then** fetch orders.
- **Then** calculate total.
- **Then** print the result.
Ensure the main thread doesn't exit before the async work is done (use `join()` at the end for testing).

<details>
<summary><b>Click to Reveal Solution</b></summary>

### Solution
Use `supplyAsync` and `thenCompose` (because getOrders likely returns a Future too, or simulates a dependent action). If getOrders was synchronous, `thenApply` would work. Assuming we want a pure async chain:

```java
CompletableFuture.supplyAsync(() -> getUser(1))
    .thenCompose(user -> CompletableFuture.supplyAsync(() -> getOrders(user)))
    .thenApply(order -> calculateTotal(order))
    .thenAccept(total -> System.out.println("Total: $" + total))
    .join(); // Block main thread so we see output
```
</details>

---

## Lab 5: The Million User Challenge (Virtual Threads)
**Concept:** Scalability & Scalable Thread Safety

### The Scenario
You want to simulate 100,000 clients connecting to your server. 
1. Each client waits for 1 second (simulating network latency).
2. We want to **count** how many clients successfully connected.

### The Problem Code
Try to do this with standard threads. **Warning: This might crash your JVM if you go too high!**
Also, notice the `count++` — is it safe?

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrashTest {
    static int visitCount = 0; // Shared Counter

    public static void main(String[] args) {
        // Standard Threads (Heavy!)
        try (ExecutorService executor = Executors.newCachedThreadPool()) {
            for (int i = 0; i < 100_000; i++) {
                 executor.submit(() -> {
                     try { Thread.sleep(1000); } catch (Exception e) {}
                     visitCount++; // NOT THREAD SAFE!
                 });
            }
        } // Auto-close waits for tasks
        System.out.println("Finished! Total Visits: " + visitCount);
    }
}
```

### The "Bug"
1. **Crash:** Creating 100k OS threads requires ~200GB of RAM (2MB per thread).
2. **Race Condition:** Even if it runs, `visitCount++` is not atomic. You will lose counts.

### Your Task
1. Fix the crash using **Virtual Threads** (Java 21+).
2. Fix the counter using `AtomicInteger` so we get exactly 100,000.

<details>
<summary><b>Click to Reveal Solution</b></summary>

### Solution
Change the Executor to `newVirtualThreadPerTaskExecutor` and use `AtomicInteger`.

```java
import java.util.concurrent.atomic.AtomicInteger;

public class SafeVirtualThreads {
    static AtomicInteger visitCount = new AtomicInteger(0);

    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 100_000; i++) {
                    executor.submit(() -> {
                        try { Thread.sleep(1000); } catch (Exception e) {}
                        visitCount.incrementAndGet(); // Thread Safe!
                    });
            }
        }
        System.out.println("Finished! Total Visits: " + visitCount.get());
    }
}
```
**Result:** Runs in ~1 second, uses minimal RAM, and prints exactly 100,000.
</details>

---

## Lab 6: The Slow Dashboard (Parallel Aggregation)
**Concept:** `CompletableFuture.allOf` & Combining Results

### The Scenario
You are building a User Dashboard that needs to fetch data from 3 independent microservices:
1.  **Impatient User Profile** (1 sec)
2.  **Recent Orders** (1 sec)
3.  **Product Recommendations** (1 sec)

You need to combine all 3 into a final `Dashboard` object.

### The Problem Code
The current implementation calls them one by one. The user has to wait **3 seconds**.

```java
import java.util.concurrent.CompletableFuture;

public class DashboardLoader {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Sequential (Blocking)
        String profile = getProfile(); // 1s
        String orders = getOrders(); // 1s
        String suggestions = getSuggestions(); // 1s

        System.out.println("Dashboard Loaded: " + profile + " | " + orders + " | " + suggestions);
        
        long end = System.currentTimeMillis();
        System.out.println("Total Time: " + (end - start) + "ms");
    }

    static String getProfile() { sleep(1000); return "User Profile"; }
    static String getOrders() { sleep(1000); return "5 Orders"; }
    static String getSuggestions() { sleep(1000); return "Buy This!"; }
    
    static void sleep(int ms) { try { Thread.sleep(ms); } catch (Exception e) {} }
}
```

### Your Task
Make this run in **~1 second**.
1.  Launch all 3 tasks asynchronously.
2.  Wait for **ALL** of them to finish.
3.  Combine the results into the print statement.

<details>
<summary><b>Click to Reveal Solution</b></summary>

### Solution
Use `CompletableFuture.supplyAsync` to start them, and `CompletableFuture.allOf` (or just join them independently if you don't need to trigger a single callback) to wait.

```java
CompletableFuture<String> pFuture = CompletableFuture.supplyAsync(() -> getProfile());
CompletableFuture<String> oFuture = CompletableFuture.supplyAsync(() -> getOrders());
CompletableFuture<String> sFuture = CompletableFuture.supplyAsync(() -> getSuggestions());

// Wait for all (Optional, but good for structured concurrency concepts)
CompletableFuture.allOf(pFuture, oFuture, sFuture).join(); 

// Fetch results (now ready)
System.out.println("Dashboard Loaded: " + 
    pFuture.join() + " | " + 
    oFuture.join() + " | " + 
    sFuture.join()
);
```
**Advanced (One Pipeline):**
If you want a single Future that returns the result:
```java
pFuture.thenCombine(oFuture, (profile, orders) -> profile + " | " + orders)
       .thenCombine(sFuture, (partial, suggestions) -> partial + " | " + suggestions)
       .thenAccept(System.out::println)
       .join();
```
</details>
