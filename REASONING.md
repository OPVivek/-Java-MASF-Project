# 🧠 Reasoning: Engineering Tradeoffs & Interview Preparation

This document outlines the core technical decisions, design tradeoffs, and interview talking points behind the **MASF** architecture.

---

## ⚖️ Key Architectural Decisions & Tradeoffs

### 1. Why Pure Java Standard Library instead of Frameworks (Spring Boot / Celery)?
* **Cognitive Transparency**: Demonstrates fundamental mastery of the Java Memory Model (JMM), threads, and synchronization primitives without framework abstraction hiding the complexity.
* **Zero Overhead & Footprint**: Boots instantly with zero external dependencies, making compilation and testing instantaneous in memory-constrained or benchmark environments.
* **Direct Relevance to Systems Engineering**: Evaluators at infrastructure companies like Salesforce look for deep comprehension of core concurrency APIs (`ExecutorService`, `BlockingQueue`, `CountDownLatch`) rather than basic configuration annotations.

---

### 2. Why a Custom `PriorityFutureTask` with `ThreadPoolExecutor`?
* **The Problem**: Standard Java `ThreadPoolExecutor.submit(Runnable)` wraps tasks inside an internal `FutureTask`. Because standard `FutureTask` does not implement `Comparable`, submitting prioritizable tasks to a `PriorityBlockingQueue` causes a `ClassCastException`!
* **The Solution**: We extended `ThreadPoolExecutor` and overrode `newTaskFor(Runnable, T)` to return a custom `PriorityFutureTask` that delegates `compareTo()` to the wrapped task.
* **Interview Impact**: This is an advanced Java concurrency pattern that separates junior candidates from strong systems developers.

---

### 3. Why `CountDownLatch` instead of `Thread.sleep()` or Active Polling?
* **Zero Guesswork**: `Thread.sleep()` is either too short (causing race conditions and premature reporting) or too long (wasting CPU cycles and inflating execution times).
* **Deterministic Synchronization**: A `CountDownLatch` synchronized to the task count allows worker threads to release the barrier the exact millisecond the final task resolves, achieving sub-second execution ($\sim$750ms).

---

### 4. Why the Template Method Design Pattern?
* **Invariance of Invariants**: Security, state validation, timing metrics, and error handling must be executed identically across every task type.
* **Enforcing Invariants**: By declaring `AbstractWorkerTask.run()` as `final`, subclasses cannot tamper with state transitions or skip audit logging; they can only implement the core payload in `doWork()`.

---

## 🎯 Interview Deep-Dive: Questions & Model Answers

### Q1: *"How does your engine handle tasks with the same priority?"*
> **Answer**:  
> *"We implemented a compound comparator in `AbstractWorkerTask.compareTo()`. The primary comparator compares numeric priority (1 being highest). If priorities are equal, it falls back to the unique `BaseEntity.getId()`, which is backed by a thread-safe `AtomicLong`. This guarantees deterministic FIFO ordering among tasks of equal priority with no collisions."*

---

### Q2: *"How do you prevent thread visibility issues across multiple worker threads?"*
> **Answer**:  
> *"We apply three techniques from the Java Memory Model:  
> 1. The `state` variable in `BaseTask` is marked `volatile` to establish a happens-before relationship between writes and reads across CPU caches.  
> 2. Critical mutations (such as duration computation and priority assignments) are enclosed in synchronized accessors.  
> 3. Task state observers and task registries leverage `CopyOnWriteArrayList`, guaranteeing safe concurrent reads without throwing `ConcurrentModificationException`."*

---

### Q3: *"How would you evolve this engine into a distributed system?"*
> **Answer**:  
> *"Currently, MASF orchestrates tasks concurrently on a single JVM. To scale horizontally across multiple instances:  
> 1. Replace the in-memory `PriorityBlockingQueue` with a distributed message broker like Apache Kafka or Redis Sorted Sets.  
> 2. Introduce a distributed lock (e.g. via Redis/ZooKeeper) for state transitions.  
> 3. Stream state logs into a centralized logging pipeline like Elasticsearch or Kafka."*
