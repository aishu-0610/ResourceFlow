# ResourceFlow — Syllabus Mapping (23XT58 Java Programming Lab)

Where each syllabus topic appears in the code, and one sentence you can say about it.
All paths are under `src/main/java/com/resourceflow/` unless they start with `src/test`.
Line numbers are where to point on screen.

---

## The 30-second architecture

```
JavaFX screens (ui/)  ─┐
                       ├─> Services (service/) ─> AllocationEngine (engine/) ─> Strategies (strategy/)
Console menu (console/)┘          │                        │
                                  ▼                        ▼
                     SQLite via JDBC (repository/)   Metrics + comparison (evaluation/)
```

Two front ends (GUI and console) share the same services and engine. That separation is the
main design idea: the algorithms know nothing about JavaFX.

---

## Unit 1 — Java Programming fundamentals

| Topic | Where | What to say |
|---|---|---|
| Data types, operators | `model/EvaluationMetrics.java:142` `weightedScore()` | `double` arithmetic combines three objectives into one score. |
| Control structures | `console/ConsoleApp.java:60` (switch), `strategy/FairShareAllocationStrategy.java` (while/for/if) | Arrow-style `switch` picks the menu action; the water-filling loop repeats until capacity runs out. |
| Arrays | `console/ConsoleApp.java:28` `String[] MENU`, `StrategyType.values()` | Menu items live in an array and are printed with an indexed `for` loop. |
| Strings | `io/ReportExporter.java` `escape()`, `String.format` everywhere | CSV escaping uses `contains`, `replace`, string concatenation. |
| Input/Output | `console/ConsoleApp.java:42`, `io/` package | See Unit 6. |
| Classes, constructors | `model/Resource.java`, `model/AllocationRequest.java` | Entities with private fields, overloaded constructors, getters/setters. |
| `this` keyword | every constructor, e.g. `engine/ParallelStrategyRunner.java` constructor | `this.planner = planner` separates the field from the parameter. |
| Scope rules | `strategy/PriorityAllocationStrategy.java:76` | `available` exists only inside `allocateOne`; `remainingCapacity` is passed in because it must outlive one request. |
| Object-based vs object-oriented | `strategy/` package | Object-based = objects only (like `Resource`). Object-oriented adds inheritance + polymorphism (the strategy hierarchy). |
| Inheritance, reusability | `strategy/ShareBasedAllocationStrategy.java` | Fair-Share and Proportional reuse the grouping and constraint-pruning code; each only supplies `computeShares()`. |
| Composing class (composition) | `engine/AllocationEngine.java` | The engine *has a* validator, evaluator, explainer and reallocator rather than inheriting from them. |
| Method overloading | `engine/AllocationEngine.java:102` and `:121`, `util/AuditLog.java:80` | `executeAllocation` with/without a config; `compareAllStrategies` with/without a listener; `record(msg)` vs `record(format, args...)`. |
| Abstract classes | `strategy/AbstractAllocationStrategy.java:71`, `strategy/ShareBasedAllocationStrategy.java:36` | Abstract methods force every subclass to provide the algorithm-specific step. |
| Dynamic method dispatch | `engine/AllocationEngine.java:87` `strategy.allocate(...)` | The variable's type is the interface; the JVM chooses the subclass's code at runtime. |

**Class hierarchy (multilevel inheritance):**

```
AllocationStrategy                      (interface)
└── AbstractAllocationStrategy          (abstract — template method allocate())
    ├── PriorityAllocationStrategy      (greedy, PriorityQueue)
    └── ShareBasedAllocationStrategy    (abstract — grouping + constraint pruning)
        ├── FairShareAllocationStrategy (max-min water-filling)
        └── ProportionalAllocationStrategy (demand × priority weights)
```

---

## Unit 2 — Packages and Interfaces

| Topic | Where | What to say |
|---|---|---|
| Packages | `model`, `strategy`, `engine`, `service`, `repository`, `io`, `console`, `ui`, `util`, `exception` | One package per layer; a package's name says what it is responsible for. |
| Access protection | `strategy/AbstractAllocationStrategy.java` | `private` fields, `protected` helpers for subclasses only, `public` API, `final` methods subclasses cannot override. `ReportExporter.escape()` is package-private so the test in the same package can call it. |
| Importing packages | top of every file | e.g. `import java.util.*;` vs single-type imports. |
| Defining / implementing an interface | `strategy/AllocationStrategy.java`, `repository/ResourceRepository.java` | Repositories are interfaces; `SqliteResourceRepository` is one implementation (could swap for another DB). |
| Applying interfaces | `engine/AllocationEngine.java` `Map<StrategyType, AllocationStrategy>` | The engine stores any strategy behind the interface. |
| Variables in interfaces | `strategy/AllocationStrategy.java:23` `double EPSILON` | Interface fields are implicitly `public static final`, so all strategies share one tolerance. |
| Default methods | `strategy/AllocationStrategy.java:43` `getComplexity()` | Adding a method to an interface without breaking existing implementations. |

---

## Unit 3 — Exception Handling

| Topic | Where | What to say |
|---|---|---|
| Exception types: checked vs unchecked | `exception/ScenarioFileException.java:12` (extends `Exception`) vs `exception/ResourceFlowException.java:6` (extends `RuntimeException`) | File errors are checked so the compiler forces callers to handle them; domain rule errors are unchecked. |
| Creating your own subclasses | `exception/` package (7 classes) | A hierarchy under `ResourceFlowException` so callers can catch broadly or precisely. |
| try / catch | `ui/views/WhatIfAnalysisView.java:342` `applyEdit()` | Typing "abc" as a capacity is caught and shown in a dialog. |
| Multiple catch clauses | `io/ScenarioFileStore.java` `load()` | Separate catches for missing file, wrong version, damaged file, other I/O. |
| Multi-catch | `io/ScenarioFileStore.java:63` `ClassNotFoundException \| ClassCastException \| InvalidClassException` | One handler for several unrelated exception types. |
| Nested try | `io/ScenarioFileStore.java:46` | An inner try-with-resources inside an outer try that translates errors. |
| `throw` | `service/CapacityTransfer.java` | Throws `ResourceValidationException` when a transfer breaks a rule. |
| `throws` | `io/ReportExporter.java:31`, `io/ScenarioFileStore.java:28` | The method declares the checked exception; the caller decides how to report it. |
| Built-in exceptions | `NumberFormatException` (`console/ConsoleApp.java:189`), `IllegalArgumentException` (model setters), `InterruptedException` (threads) | Reused Java exceptions where they already describe the problem. |
| Chained exceptions | `engine/ParallelStrategyRunner.java:62`, `io/ScenarioFileStore.java`, `ui/views/WhatIfAnalysisView.java:436` | The low-level cause is kept with `new X(message, cause)` and shown to the user with `getCause()`. |
| Uncaught exceptions | `Launcher.java:24`, `MainApp.java:36` | A default handler logs anything nobody caught and shows a dialog instead of failing silently. |
| try-with-resources | `repository/SqliteScenarioRepository.java:30`, `util/AuditLog.java` | Connections, statements and files are closed automatically. |

---

## Unit 4 — Multithreaded Programming

| Topic | Where | What to say |
|---|---|---|
| Main thread | `Launcher.java`, `console/ConsoleApp.java` prints `Thread.currentThread().getName()` | The JVM starts `main()` on the thread called "main". |
| Thread class + Runnable | `engine/ParallelStrategyRunner.java:92` `StrategyWorker implements Runnable`, `:49` `new Thread(...).start()` | Strategy Comparison runs each strategy on its own thread. |
| Priorities | `engine/ParallelStrategyRunner.java:47` (above normal), `util/AuditLog.java:38` (`MIN_PRIORITY`) | The user is waiting for comparisons; disk logging is background work. |
| `join()` | `engine/ParallelStrategyRunner.java:54` | The caller waits for all workers before building the comparison. |
| Synchronization | `engine/ParallelStrategyRunner.java:78` `synchronized` methods; `service/CapacityTransfer.java:43` `synchronized` blocks | Only one thread at a time may add to the shared list / change two resources. |
| Inter-thread communication (wait / notifyAll) | `util/EventBuffer.java:30` `put()`, `:38` `take()` | Producer–consumer: producers wait while the buffer is full, the consumer waits while it is empty. |
| Messaging between threads | `util/AuditLog.java` | Any thread drops a message in the buffer; one writer thread saves it to disk. |
| Deadlock (and avoidance) | `service/CapacityTransfer.java`, test `src/test/.../service/CapacityTransferTest.java` | Two threads moving capacity A→B and B→A would deadlock; locking in a fixed ID order prevents it. The test runs 20,000 opposing transfers on two threads. |
| Thread pools / UI thread | `ui/util/TaskRunner.java:17`, `Platform.runLater` in `ui/views/StrategyComparisonView.java:295` | Long work runs off the JavaFX thread; results are handed back to it because only the FX thread may touch controls. |

**Live demo:** Strategy Comparison → "Run All Strategies Benchmark". The *Parallel Execution Log*
shows which thread finished which strategy. The order changes between runs, which shows they really run concurrently.

---

## Unit 5 — Collections Framework

| Topic | Where | What to say |
|---|---|---|
| Generics (using) | `List<AllocationRequest>`, `Map<String, Double>` everywhere | Type-safe collections, no casts. |
| Generics (writing your own) | `util/EventBuffer.java` `EventBuffer<T>` | One buffer class works for any item type (`String` in AuditLog, `Integer` in the test). |
| Autoboxing / unboxing | `strategy/PriorityAllocationStrategy.java:76`, `src/test/.../util/AuditLogTest.java` | `double` goes into `Map<String, Double>` as a `Double` and comes back out automatically. |
| List | `ArrayList` in every strategy | Ordered results. |
| Set | `HashSet` in `ShareBasedAllocationStrategy` (excluded requests) | Fast "already excluded?" checks. |
| Queue | `PriorityQueue` at `strategy/PriorityAllocationStrategy.java:47`; `ArrayDeque` in `EventBuffer` | The priority queue always hands back the most urgent request next. |
| Map | `EnumMap` in `AllocationEngine`, `LinkedHashMap` grouping in `ShareBasedAllocationStrategy` | Strategy lookup by enum; requests grouped per resource in stable order. |
| Comparator / Comparable | `strategy/PriorityAllocationStrategy.java` `SERVICE_ORDER` | Priority, then arrival time, then size, then ID. |

---

## Unit 6 — I/O and GUI Development

| Topic | Where | What to say |
|---|---|---|
| Streams, stream classes (byte) | `io/ScenarioFileStore.java` | `Files.newOutputStream` → `BufferedOutputStream` → `ObjectOutputStream`. |
| Character streams | `io/ReportExporter.java`, `util/AuditLog.java` | `BufferedWriter` / `PrintWriter` / `FileWriter` for text. |
| Object Serialization | `io/ScenarioFileStore.java:28` / `:46`, `model/Scenario.java:10` `implements Serializable` | What-If → Export/Import Scenario File saves the whole sandbox to a `.rflow` file and loads it back. |
| NIO classes | `java.nio.file.Path`, `Files.newBufferedWriter`, `Files.readAllLines` (`util/AuditLog.java:92`), `Files.createDirectories` | Modern file API used for reports, logs and scenario files. |
| Predefined streams | `System.in`, `System.out`, `System.err` in `console/ConsoleApp.java` | Errors go to `System.err`, normal output to `System.out`. |
| Reading/writing console input | `console/ConsoleApp.java:42` `BufferedReader(new InputStreamReader(System.in))` | Run `java -jar target/resourceflow-1.0.0.jar --console`. |
| JavaFX basics, UI controls | `ui/views/*` | TableView, ComboBox, Slider, ListView, TextArea, CheckBox, charts, FileChooser. |
| Event-driven programming | `ui/views/StrategyComparisonView.java:164` slider listener; `setOnAction`, `setOnEditCommit` in `WhatIfAnalysisView` | Moving a weight slider re-ranks strategies instantly; double-clicking a cell edits capacity or priority. |
| Applets, AWT, Swing, JavaScript | not used | Applets were removed from Java (JDK 11+). JavaFX replaces AWT/Swing for new desktop apps and is what the project brief specifies. |
| Native methods | used indirectly: the SQLite JDBC driver loads a native C library through JNI | The JVM warning `System::load ... SQLiteJDBCLoader` at startup is that native library being loaded. |

---

## Unit 7 — New Features (Annotations, Functional Programming, Lambdas)

| Topic | Where | What to say |
|---|---|---|
| Custom annotation | `strategy/StrategyInfo.java`; used at `strategy/PriorityAllocationStrategy.java:17` | `@StrategyInfo(name, description, complexity)` with `RUNTIME` retention. |
| Reading annotations (reflection) | `strategy/AbstractAllocationStrategy.java:31` `getClass().getAnnotation(...)` | Name/description/complexity are read from the annotation, not hard-coded; the "Complexity" column in Strategy Comparison comes from here. |
| Built-in annotations | `@Override`, `@FunctionalInterface`, JUnit `@Test`, `@Timeout`, `@TempDir` | |
| Functional interface | `engine/AllocationListener.java:14` | Exactly one abstract method, so a lambda can implement it. |
| Lambda expressions | `console/ConsoleApp.java:111`, `ui/views/StrategyComparisonView.java` listeners | `(plan, ms, thread) -> System.out.printf(...)`. |
| Method references | `AllocationRequest::getPriority`, `this::writeLoop`, `this::showScenario` | Short form of a lambda that calls one method. |
| Streams API (functional style) | `strategy/ShareBasedAllocationStrategy.java` `groupingBy`, `evaluation/StrategyComparison.java:72` `max(...)` | Declarative filtering, grouping and finding the best strategy. |

---

## Tutorial Practice

### 1. Runtime polymorphism using abstract class and interface

- **Interface:** `strategy/AllocationStrategy.java`
- **Abstract class:** `strategy/AbstractAllocationStrategy.java` (and `ShareBasedAllocationStrategy.java`)
- **Concrete classes:** Priority, Fair-Share, Proportional
- **Where polymorphism happens:** `engine/AllocationEngine.java:87` calls `strategy.allocate(...)` on an
  `AllocationStrategy` reference. Which `computeItems()` / `computeShares()` runs is decided at runtime.
- **Proof:** `src/test/java/com/resourceflow/strategy/StrategyHierarchyTest.java`, test
  `dynamicDispatchPicksTheSubclassAlgorithm`: the same loop over interface references produces three different allocations.

### 2. Callback feature using interface

- **Callback interface:** `engine/AllocationListener.java`
- **Caller registers it:** `ui/views/StrategyComparisonView.java` (GUI) and `console/ConsoleApp.java:111` (console) pass a lambda.
- **Engine calls it back:** `engine/ParallelStrategyRunner.java` → `listener.onStrategyFinished(plan, ms, threadName)`
  as each worker thread finishes.
- **Proof:** `src/test/java/com/resourceflow/engine/ParallelComparisonTest.java`, test `callbackReportsEachWorkerThread`.

---

## Suggested 5-minute demo

1. **Dashboard** → overview of resources and requests (demo data is seeded on first launch).
2. **Allocation Workspace** → pick *Priority-Based*, Execute → table with explanations → **Export CSV** (open it in Excel).
3. **Strategy Comparison** → Run benchmark → point at the *Parallel Execution Log* (threads + callback) and the
   *Complexity* column (annotation). Drag the **Fairness** slider to 1 and the others to 0: the recommendation
   changes instantly (event-driven).
4. **What-If Analysis** → double-click a capacity, type `abc` (exception dialog), then `200` → **Move** 20 Hours
   from GPU-H100 to GPU-A100 (deadlock-safe transfer) → Simulate → **Export Scenario File**, then **Import** it back (serialization).
5. **Settings & Diagnostics** → Audit Log card: lines written by the background thread, each tagged with the thread that recorded it.
6. Terminal: `java -jar target/resourceflow-1.0.0.jar --console` → option 3 (parallel comparison in text mode).
7. `mvn test` → 39 tests pass, including the deadlock test and the serialization round trip.

---

## Likely viva questions (short answers)

- **Why an abstract class *and* an interface?** The interface is the contract the engine depends on; the abstract
  class shares code between implementations. A class can implement many interfaces but extend only one class.
- **Why is `allocate()` `final`?** So no subclass can skip the shared steps (null config, empty input); subclasses
  customise only `computeItems()`. This is the Template Method pattern.
- **`wait()` vs `sleep()`?** `wait()` releases the lock and waits for `notify`; `sleep()` keeps holding any lock and just pauses.
- **Why `while` around `wait()`, not `if`?** A thread can wake up when the condition is still false (spurious wakeup or another
  thread got there first), so it must re-check.
- **How is deadlock prevented?** Every thread locks the two resources in the same order (smaller ID first), so a cycle of waiting threads cannot form.
- **Checked vs unchecked?** Checked (extends `Exception`) must be caught or declared; unchecked (extends `RuntimeException`) need not be.
- **What does `serialVersionUID` do?** It is a version stamp; if the class changes incompatibly, reading an old file fails with `InvalidClassException`, which we catch.
- **Why `Platform.runLater`?** JavaFX controls may only be changed on the JavaFX Application Thread; worker threads hand the update over.
