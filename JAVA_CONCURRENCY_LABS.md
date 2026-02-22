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

---

## Lab 4: The Order Processor (CompletableFuture)
**Concept:** Asynchronous Composition

### The Scenario
You need to process an order by:
1.  Fetching the User (500ms)
2.  Getting their Orders (Wait for Step 1, then 500ms)
3.  Calculating the Total (Wait for Step 2, then Instant)

### The Problem Code
Copy this into `OrderProcessor.java`. It demonstrates the slow, synchronous way.

```java
public class OrderProcessor {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // 1. Fetch User (500ms)
        User user = getUser(1);
        // 2. Fetch Orders (500ms)
        Order order = getOrders(user);
        // 3. Calculate Total (Instant)
        int total = calculateTotal(order);
        
        System.out.println("Processed for " + user.name + ": $" + total);
        
        long end = System.currentTimeMillis();
        System.out.println("Total Time: " + (end - start) + "ms");
    }

    // --- Mock Fetching Methods ---
    static User getUser(int id) {
        sleep(500);
        return new User(id, "John Doe");
    }

    static Order getOrders(User user) {
        sleep(500);
        return new Order(user.id, 250);
    }

    static int calculateTotal(Order order) {
        return order.price + 50; // + $50 shipping
    }

    static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}

class User {
    int id; String name;
    User(int id, String name) { this.id = id; this.name = name; }
}

class Order {
    int userId; int price;
    Order(int userId, int price) { this.userId = userId; this.price = price; }
}
```

### Your Task
Rewrite this using `CompletableFuture`. 
- Fetch user asynchronously.
- **Then** fetch orders.
- **Then** calculate total.
- **Then** print the result.
Ensure the main thread doesn't exit before the async work is done (use `join()` at the end for testing).
---

## Lab 5: The Million User Challenge (Virtual Threads)
**Concept:** Scalability & Scalable Thread Safety

### The Scenario
You want to simulate 100,000 clients connecting to your server *at the same time*. 
1. Each client waits for 1 second (simulating network latency).
2. We want to **count** how many clients successfully connected.

**The Goal:** See why "One Thread Per Request" doesn't scale for high concurrency, and how to count safely.

### The Problem Code
Try to do this with standard threads. **Warning: This might crash your JVM if you go too high!**
Also, notice the `count++` — is it safe?

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CrashTest {
    static int visitCount = 0; // Shared Counter

    public static void main(String[] args) throws InterruptedException {
        // Standard Threads (Heavy!)
        ExecutorService executor = Executors.newCachedThreadPool();
        
        for (int i = 0; i < 100_000; i++) {
             executor.submit(() -> {
                 try { Thread.sleep(1000); } catch (Exception e) {}
                 visitCount++; // NOT THREAD SAFE!
             });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("Finished! Total Visits: " + visitCount);
    }
}
```

### Your Task
1. **Scale:** Use **Virtual Threads** to handle 100k concurrent connections without crashing (they use bytes of RAM, not MBs).
2. **Thread Safety:** Use `AtomicInteger` to ensure the count is exactly 100,000.


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

```

---
