# Java Concurrency — MCQ Questions

Test your understanding of Java concurrency concepts. Answers are in [MCQ_ANSWERS.md](MCQ_ANSWERS.md).

---

## Module 1: Fundamentals & Core Concepts

**Q1.** What are the two primary reasons for using concurrency in software?

- A) Security and Encryption
- B) Responsiveness and Performance
- C) Code readability and Maintainability
- D) Logging and Monitoring

---

**Q2.** What is the difference between **Concurrency** and **Parallelism**?

- A) They are the same thing
- B) Concurrency is about doing tasks simultaneously on multiple cores; Parallelism is about dealing with multiple tasks on a single core
- C) Concurrency is about dealing with multiple things at once (structure); Parallelism is about doing multiple things at once (execution)
- D) Concurrency requires multi-core CPUs; Parallelism works on single-core CPUs

---

**Q3.** Which resource is **NOT** shared between threads within the same process?

- A) Heap Memory
- B) Stack
- C) Static variables
- D) Code segment

---

**Q4.** Why did Google Chrome choose to run each browser tab as a separate **process** instead of a thread?

- A) Processes are faster than threads
- B) To save memory
- C) For crash isolation and security — one tab crash won't kill others
- D) Because JavaScript requires separate processes

---

**Q5.** What happens during a **context switch**?

- A) The CPU shuts down and restarts
- B) The current task's state is saved, and the next task's state is loaded
- C) All threads are terminated and restarted
- D) The JVM garbage collector runs

---

**Q6.** In the Java Memory Model, why might Thread B **not see** a variable change made by Thread A?

- A) Thread B is lower priority
- B) Thread A hasn't called `System.gc()`
- C) Thread A's update may be cached in its local working memory and not flushed to main memory
- D) Variables are always visible across threads immediately

---

**Q7.** Why is `counter++` considered **NOT atomic**?

- A) It uses floating point arithmetic
- B) It is actually three operations: Read, Add, Write — which can be interleaved by other threads
- C) The `++` operator is deprecated
- D) It only works on `long` types

---

## Module 2: Thread Management & Safety

**Q8.** What is the critical difference between calling `thread.start()` and `thread.run()`?

- A) `start()` is slower than `run()`
- B) `run()` creates a new thread; `start()` does not
- C) `start()` creates a new OS thread; `run()` executes the code on the current thread like a normal method
- D) There is no difference

---

**Q9.** How should you properly stop a running thread in Java?

- A) Call `thread.stop()`
- B) Call `thread.destroy()`
- C) Call `thread.interrupt()` and have the thread check `isInterrupted()` or handle `InterruptedException`
- D) Set the thread to `null`

---

**Q10.** A thread is in the **BLOCKED** state. What is it waiting for?

- A) A `Thread.sleep()` timer to expire
- B) Another thread to call `notify()`
- C) An intrinsic lock (monitor) held by another thread
- D) User input from the console

---

**Q11.** What does the `synchronized` keyword guarantee?

- A) Only that the code runs faster
- B) Mutual exclusion — only one thread can execute the synchronized block at a time (on the same lock)
- C) That no exceptions will be thrown
- D) That the thread runs at highest priority

---

**Q12.** What is a **Deadlock**?

- A) A thread that consumes 100% CPU
- B) Two or more threads waiting for each other's locks, forming a circular dependency — none can proceed
- C) A thread that finishes too quickly
- D) A thread that throws an unchecked exception

---

**Q13.** How can you prevent a deadlock?

- A) Use more threads
- B) Always acquire locks in the **same order** across all threads
- C) Use `Thread.sleep()` between lock acquisitions
- D) Set thread priorities

---

**Q14.** What does the `volatile` keyword guarantee?

- A) Atomicity and Visibility
- B) Only Visibility — changes are flushed to main memory, but compound operations like `count++` are still not atomic
- C) Only Atomicity
- D) Thread priority

---

**Q15.** What advantage does `ReentrantLock` have over `synchronized`?

- A) It is always faster
- B) It supports try-locking with timeouts, interruptible locking, and fairness policies
- C) It doesn't require `unlock()`
- D) It automatically prevents deadlocks

---

**Q16.** What is a **Livelock**?

- A) Threads are blocked and idle
- B) Threads are active and responding to each other, but making no actual progress (like two people side-stepping in a hallway)
- C) A thread that never starts
- D) A thread that holds too many locks

---

**Q17.** Why must you call `threadLocal.remove()` in thread pool environments?

- A) To improve performance
- B) Because the thread lives forever in a pool — without `remove()`, the old value leaks and persists for the next task
- C) It is not necessary
- D) To make the thread faster

---

## Module 3: Building Blocks & Patterns

**Q18.** Why should you use an `ExecutorService` instead of `new Thread()` directly?

- A) `ExecutorService` is slower but safer
- B) Thread pools **reuse** threads, avoiding the expensive overhead of creating/destroying OS threads per task
- C) `new Thread()` doesn't work in Java 21
- D) `ExecutorService` doesn't require `shutdown()`

---

**Q19.** What is the difference between `Runnable` and `Callable`?

- A) They are identical
- B) `Runnable.run()` returns `void`; `Callable.call()` can return a value and throw checked exceptions
- C) `Callable` is older than `Runnable`
- D) `Runnable` can only be used with thread pools

---

**Q20.** What happens when you call `future.get()` on a `Future` that hasn't completed yet?

- A) It returns `null` immediately
- B) It throws an exception
- C) It **blocks** the calling thread until the result is available
- D) It cancels the task

---

**Q21.** Why is `ConcurrentHashMap` preferred over `Collections.synchronizedMap()`?

- A) It uses no locking at all
- B) It uses **Lock Stripping** — locking only individual segments/buckets, allowing concurrent access to different parts of the map
- C) It is simpler to use
- D) It is only available in Java 21+

---

**Q22.** When is `CopyOnWriteArrayList` the best choice?

- A) When you write to the list frequently
- B) When reads vastly outnumber writes — it creates a new copy of the array on every modification, so reads never block
- C) When the list contains more than 1 million items
- D) When you need sorted order

---

**Q23.** In the Producer-Consumer pattern, what does `BlockingQueue.put()` do when the queue is full?

- A) Throws an exception
- B) Drops the item silently
- C) **Blocks** the producer thread until space becomes available
- D) Overwrites the oldest item

---

**Q24.** In the Fork/Join Framework, what is **Work Stealing**?

- A) A thread deletes work from another thread's queue
- B) An idle thread **takes** pending tasks from a busy thread's deque to utilize all CPU cores efficiently
- C) The JVM reduces thread count automatically
- D) A security vulnerability

---

## Module 4: Modern Async Programming

**Q25.** What is the key advantage of `CompletableFuture` over `Future`?

- A) `CompletableFuture` is faster
- B) `CompletableFuture` supports **non-blocking** chaining (`thenApply`, `thenCompose`) — you don't need to call blocking `get()`
- C) `Future` is removed in Java 21
- D) `CompletableFuture` doesn't need an executor

---

**Q26.** What is the difference between `thenApply()` and `thenCompose()`?

- A) They are identical
- B) `thenApply` transforms a result synchronously (like `map`); `thenCompose` chains another async operation (like `flatMap`)
- C) `thenCompose` is faster
- D) `thenApply` only works with Strings

---

**Q27.** What does `CompletableFuture.allOf(cf1, cf2, cf3)` do?

- A) Cancels all futures
- B) Returns a new `CompletableFuture` that completes when **all** the given futures complete
- C) Returns the fastest result
- D) Runs the futures sequentially

---

**Q28.** Why should you pass your own `Executor` to `CompletableFuture.supplyAsync()` in production code?

- A) The default executor is buggy
- B) The default `ForkJoinPool.commonPool()` is shared across the entire application — a slow task can starve other work
- C) Custom executors are faster
- D) It is required by Java 21

---

## Module 5: Advanced Topics & Best Practices

**Q29.** For a **CPU-bound** task (e.g., heavy calculations), what is the recommended thread pool size?

- A) As many threads as possible
- B) Approximately equal to the number of CPU cores (`N_cpu + 1`)
- C) 1 thread
- D) 1000 threads

---

**Q30.** What does `Runtime.getRuntime().availableProcessors()` return on a 4-core CPU with Hyper-Threading (SMT)?

- A) 4 (physical cores)
- B) 8 (logical processors)
- C) 2
- D) 16

---

**Q31.** In the Event Loop model (used by Spring WebFlux / Node.js), how many threads typically handle all traffic?

- A) One thread per user
- B) A very small number of threads (often equal to CPU cores), using non-blocking I/O and callbacks
- C) Exactly 1 thread always
- D) Thousands of threads

---

## Virtual Threads (Java 21+)

**Q32.** How do Virtual Threads differ from Platform Threads?

- A) Virtual threads are OS-managed; Platform threads are JVM-managed
- B) Virtual threads are lightweight (KB of heap), JVM-scheduled, and can scale to millions; Platform threads are OS-managed and consume ~1MB each
- C) Virtual threads are faster for CPU-bound work
- D) Platform threads don't exist in Java 21

---

**Q33.** What is a **Carrier Thread** in the context of virtual threads?

- A) A thread that carries data between processes
- B) The underlying **platform thread** on which a virtual thread is mounted and executed by the JVM
- C) A special daemon thread
- D) A thread created by `Thread.ofCarrier()`

---

**Q34.** What causes a virtual thread to become **pinned** to its carrier thread?

- A) Using `volatile` variables
- B) Using `synchronized` blocks, native (JNI) methods, or `Object.wait()`
- C) Using `CompletableFuture`
- D) Creating too many virtual threads

---

**Q35.** Why should you **NOT** pool virtual threads using `Executors.newFixedThreadPool()`?

- A) It causes deadlocks
- B) Virtual threads are extremely cheap to create — pooling adds unnecessary overhead and limits scalability
- C) `FixedThreadPool` doesn't accept virtual threads
- D) Virtual threads cannot be reused

---

**Q36.** When using virtual threads, how should you limit concurrent access to a shared resource (like a database)?

- A) Use a `FixedThreadPool` to limit threads
- B) Use a **Semaphore** to permit only N concurrent accesses
- C) Use `Thread.sleep()` to slow down
- D) It is not possible

---

**Q37.** Where do virtual threads allocate their stacks?

- A) On the OS's native stack
- B) On the **JVM Heap** — meaning you may need to increase `-Xmx` when running millions of virtual threads
- C) On disk
- D) In CPU registers

---

**Q38.** Which JVM tool/flag helps you detect virtual thread **pinning** in production?

- A) `jstack`
- B) `-Djdk.tracePinnedThreads=full` and JDK Flight Recorder's `jdk.VirtualThreadPinned` event
- C) `jmap`
- D) `-verbose:gc`

---

**Q39.** When should you **NOT** switch to virtual threads?

- A) When you have a high-concurrency I/O-heavy web application
- B) When your application is low-concurrency or purely **CPU-bound**
- C) When you use `ExecutorService`
- D) When you use Java 21

---

**Q40.** Why is `ThreadLocal` caching (e.g., caching a `SimpleDateFormat` instance) problematic with virtual threads?

- A) `ThreadLocal` is removed in Java 21
- B) Virtual threads are short-lived and created per task — caching on them wastes memory since each new thread gets a new copy
- C) `ThreadLocal` causes deadlocks
- D) It is not problematic
