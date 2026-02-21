# Async Testing Masterclass: Using Awaitility

Testing asynchronous code is notoriously difficult because you never know exactly *when* a task will finish. Traditional `Thread.sleep()` is brittle and slow. **Awaitility** provides a clean, DSL-like syntax to handle "flaky" async tests reliably.

---

## 1. The Concrete Scenario

Let's imagine an `OrderService` that processes orders in a background thread.

### The Code Under Test
```java
public class OrderService {
    private String status = "PENDING";

    public void processOrder(int id) {
        // Simulate background work (e.g., DB call, Email sending)
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1500); // Random delay
                this.status = "COMPLETED";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public String getStatus() { return status; }
}
```

### The Test Code (using Awaitility)
```java
import org.junit.jupiter.api.Test;
import static org.awaitility.Awaitility.*;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    @Test
    void testOrderProcessing() {
        OrderService service = new OrderService();
        
        // 1. Start the async work
        service.processOrder(123);

        // 2. Wait for completion (polls every 100ms by default)
        await()
            .atMost(5, SECONDS)
            .until(() -> service.getStatus().equals("COMPLETED"));

        // 3. Final Assertion
        assertEquals("COMPLETED", service.getStatus());
    }
}
```

---

## 2. Why Awaitility Wins over Thread.sleep()

| Feature | `Thread.sleep(2000)` | `await().atMost(2, SECONDS)...` |
| :--- | :--- | :--- |
| **Speed** | Always waits 2 full seconds. | Finishes as soon as status is "COMPLETED". |
| **Reliability** | Fails if work takes 2.1 seconds. | Flexible up to the timeout. |
| **Readability** | "Fingers crossed" waiting. | Explicitly states "I'm waiting FOR this condition". |

---

## 3. Advanced Awaitility Patterns

### Polling with Custom Intervals
Instead of checking every 100ms, check every 500ms or use a Fibonacci backbone for backoff.
```java
await()
    .atMost(10, SECONDS)
    .pollInterval(500, MILLISECONDS) 
    .until(() -> service.isReady());
```

### Checking Collections (Hamcrest Matchers)
```java
await().until(() -> repository.count(), is(greaterThan(0L)));
```

### Ignoring Exceptions
Useful when the initial calls might fail while a resource (like a DB) is still initializing.
```java
await()
    .ignoreExceptions()
    .until(() -> service.queryHealthCheck().isUp());
```

---

## 4. Best Practices
1.  **Reasonable Timeouts:** Don't set timeouts to "Infinity". Use a realistic value for your environment.
2.  **Shared State:** Ensure the variable you are checking (like `status`) is visible across threads (e.g., `volatile` or updated within a thread-safe context).
3.  **Hamcrest/AssertJ:** Combine with matching libraries for even more readable DSLs.
