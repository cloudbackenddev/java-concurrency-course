# MCQ Answer Key

Answers for [MCQ_QUESTIONS.md](MCQ_QUESTIONS.md).

---

## Module 1: Fundamentals & Core Concepts

| Q | Answer | Explanation |
| :---: | :---: | :--- |
| Q1 | **B** | Concurrency provides responsiveness (non-freezing UIs) and performance (multi-core utilization). |
| Q2 | **C** | Concurrency = structuring tasks to handle multiple things. Parallelism = executing multiple things simultaneously on separate cores. |
| Q3 | **B** | Each thread gets its own Stack (local variables, method frames). The Heap is shared. |
| Q4 | **C** | Process isolation means one tab crash or malicious site can't affect others. The trade-off is higher memory usage. |
| Q5 | **B** | The OS saves the current thread's registers/program counter and loads the next thread's saved state. |
| Q6 | **C** | The JMM allows threads to cache variables in CPU registers. Without `volatile` or `synchronized`, changes may stay in local working memory. |
| Q7 | **B** | `counter++` is Read → Add → Write. Two threads can interleave these three steps, causing lost updates. |

## Module 2: Thread Management & Safety

| Q | Answer | Explanation |
| :---: | :---: | :--- |
| Q8 | **C** | `start()` creates a new OS thread for execution. `run()` is just a normal method call on the current thread. |
| Q9 | **C** | `thread.stop()` is deprecated/unsafe. The cooperative approach is `interrupt()` + checking the interrupt flag. |
| Q10 | **C** | BLOCKED means waiting to acquire a monitor lock (to enter a `synchronized` block) held by another thread. |
| Q11 | **B** | `synchronized` provides mutual exclusion — only one thread executes the block at a time, per object lock. |
| Q12 | **B** | Deadlock is a circular wait: Thread A holds Lock 1 and wants Lock 2, while Thread B holds Lock 2 and wants Lock 1. |
| Q13 | **B** | Consistent lock ordering prevents cycles. If all threads acquire Lock 1 before Lock 2, no circular wait can form. |
| Q14 | **B** | `volatile` guarantees visibility (flush to main memory) but not atomicity. `count++` on a volatile is still unsafe. |
| Q15 | **B** | `ReentrantLock` offers `tryLock(timeout)`, interruptible waiting, and fairness — features `synchronized` lacks. |
| Q16 | **B** | Livelock: threads are active and CPU is busy, but no progress is made — like two people side-stepping endlessly. |
| Q17 | **B** | Pool threads are reused. Without `remove()`, the previous task's `ThreadLocal` data persists — a memory leak and data corruption risk. |

## Module 3: Building Blocks & Patterns

| Q | Answer | Explanation |
| :---: | :---: | :--- |
| Q18 | **B** | Thread pools reuse existing threads. Creating a new OS thread per task is expensive (memory + OS scheduling overhead). |
| Q19 | **B** | `Callable` returns a typed result and can throw checked exceptions. `Runnable.run()` returns `void`. |
| Q20 | **C** | `future.get()` is a blocking call. It halts the calling thread until the result is computed. |
| Q21 | **B** | `ConcurrentHashMap` locks individual buckets (Lock Stripping), allowing multiple threads to read/write different segments concurrently. |
| Q22 | **B** | `CopyOnWriteArrayList` clones the array on every write, making reads lock-free. Ideal for read-heavy, write-rare scenarios (e.g., listener lists). |
| Q23 | **C** | `put()` blocks the calling thread until space is available — this is the backpressure mechanism. |
| Q24 | **B** | Idle threads steal tasks from busy threads' queues, maximizing CPU utilization across all cores. |

## Module 4: Modern Async Programming

| Q | Answer | Explanation |
| :---: | :---: | :--- |
| Q25 | **B** | `CompletableFuture` supports fluent chaining (`thenApply`, `thenCompose`, `thenAccept`) — no blocking `get()` needed. |
| Q26 | **B** | `thenApply` = synchronous transformation (map). `thenCompose` = chaining another async step (flatMap). |
| Q27 | **B** | `allOf` returns a future that completes when all input futures complete. Use it for parallel fan-out. |
| Q28 | **B** | The common pool is shared JVM-wide. A slow/stuck task in one module can starve unrelated work in another. |

## Module 5: Advanced Topics & Best Practices

| Q | Answer | Explanation |
| :---: | :---: | :--- |
| Q29 | **B** | For CPU-bound work, more threads than cores means wasted context switching. `N_cpu + 1` is the sweet spot. |
| Q30 | **B** | SMT/Hyper-Threading exposes 2 logical processors per physical core. `availableProcessors()` returns the logical count. |
| Q31 | **B** | Event Loop models use a small number of threads with non-blocking I/O and callbacks to handle thousands of connections. |

## Virtual Threads (Java 21+)

| Q | Answer | Explanation |
| :---: | :---: | :--- |
| Q32 | **B** | Virtual threads are JVM-managed, use KB of heap, and can scale to millions. Platform threads are OS-managed wrappers using ~1MB each. |
| Q33 | **B** | A carrier thread is the platform (OS) thread that the JVM uses to actually execute a virtual thread. |
| Q34 | **B** | `synchronized` blocks, JNI calls, and `Object.wait()` pin the virtual thread to its carrier, blocking other virtual threads. |
| Q35 | **B** | Virtual threads are nearly free to create. Pooling them defeats their purpose — just use `newVirtualThreadPerTaskExecutor()`. |
| Q36 | **B** | Since virtual threads remove the natural thread-count limit, use a `Semaphore` to explicitly cap concurrent access to resources. |
| Q37 | **B** | Virtual thread stacks live on the JVM Heap, not native memory. Millions of virtual threads can increase heap pressure. |
| Q38 | **B** | The JVM flag `-Djdk.tracePinnedThreads=full` logs pinning events; JFR's `jdk.VirtualThreadPinned` event helps monitor in production. |
| Q39 | **B** | Virtual threads shine for I/O-bound, high-concurrency apps. CPU-bound or low-concurrency apps won't benefit. |
| Q40 | **B** | Virtual threads are created per task and discarded. Caching objects in `ThreadLocal` means each new thread re-creates the cached object — wasteful. |
