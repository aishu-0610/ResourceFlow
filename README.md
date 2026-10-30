# ResourceFlow — Adaptive Resource Allocation & Optimisation System

> A fully functional Java 21 + JavaFX 21 desktop application for allocating limited resources among competing requests, with constraint management, multi-strategy comparison, adaptive reallocation, and what-if analysis.

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Features](#3-features)
4. [Technology Stack](#4-technology-stack)
5. [Project Structure](#5-project-structure)
6. [Running the Application](#6-running-the-application)
7. [Running the Tests](#7-running-the-tests)
8. [Allocation Strategies](#8-allocation-strategies)
9. [Database Schema](#9-database-schema)
10. [UI Screens](#10-ui-screens)
11. [Syllabus Mapping](#11-syllabus-mapping)

---

## 1. Project Overview

ResourceFlow solves the **resource allocation problem**: given a set of limited resources and competing requests for those resources, how should capacity be distributed while respecting:
- Hard capacity constraints (no resource can be over-allocated)
- Priority rankings (higher-priority requests get preference)
- Fairness requirements (optional fair-share enforcement)
- Minimum guarantees (requests can specify a minimum viable allocation)
- User-defined rules (configurable via the Constraint Config UI)

The result is a complete, standalone desktop application with persistent SQLite storage, three distinct allocation algorithms, adaptive reallocation tracking, and rich data visualisation.

---

## 2. Architecture

The application follows a strict layered architecture:

```
JavaFX Desktop UI  ->  Service Layer  ->  Allocation Engine  ->  Repository Layer  ->  SQLite
```

Key layers:
- **UI Layer** (`ui/`): JavaFX views with a sidebar navigation model, built with BorderPane/VBox layouts and a dark CSS theme.
- **Service Layer** (`service/`): Business logic coordinating engine, repositories, and domain rules. `ServiceRegistry` acts as a lightweight DI container.
- **Allocation Engine** (`engine/`): Orchestrates strategy execution, constraint validation, metric evaluation, explanation generation, and adaptive diff computation.
- **Strategy Layer** (`strategy/`): Three pluggable allocation algorithms in a class hierarchy:
  `AllocationStrategy` (interface) → `AbstractAllocationStrategy` (abstract, template method) →
  `PriorityAllocationStrategy` and `ShareBasedAllocationStrategy` (abstract) → `FairShareAllocationStrategy`, `ProportionalAllocationStrategy`.
  Display metadata comes from a custom `@StrategyInfo` annotation read by reflection.
- **Repository Layer** (`repository/`): Interface-based data access with SQLite JDBC implementations.
- **Database** (`database/`): `DatabaseManager` manages connections and idempotent schema initialisation.
- **I/O** (`io/`): scenario files via Object Serialization, CSV reports via NIO.
- **Console** (`console/`): a text-mode front end over the same services (`--console`).

---

## 3. Features

| Feature | Description |
|---|---|
| **Resource Management** | Create, edit, delete resources with name, category, total/available capacity, unit |
| **Request Management** | Add allocation requests with quantity, priority (1-5), minimum guarantee, deadline |
| **3 Allocation Algorithms** | Priority-Based, Fair-Share, Proportional |
| **Constraint Validation** | Hard capacity constraints, minimum guarantees, over-allocation detection |
| **Strategy Comparison** | Runs all 3 strategies on separate threads; a callback reports each one as it finishes |
| **Weighted Recommendation** | Utilisation / fulfilment / fairness weights (sliders) combine into one score that picks a strategy |
| **Adaptive Reallocation** | Detect changes in inventory/requests and compute impact diff |
| **What-If Analysis** | Edit capacities, demands and priorities in a sandbox, move capacity between resources (deadlock-safe), export/import scenarios as `.rflow` files |
| **CSV Export** | Export any allocation result to a spreadsheet |
| **Audit Log** | Background writer thread (producer–consumer, `wait`/`notifyAll`) records every run to `audit.log` |
| **Console Mode** | `java -jar resourceflow-1.0.0.jar --console`: text menu over the same engine |
| **Decision Explanations** | Per-request rationale for why each allocation decision was made |
| **History & Audit** | Full allocation run history persisted to SQLite |
| **Dashboard** | Live KPI metrics with JavaFX bar charts and progress indicators |
| **Settings & Diagnostics** | DB path info, demo data seed, colour theme tokens |

---

## 4. Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 (LTS) |
| UI Framework | JavaFX 21.0.2 (Controls, Charts, FXML) |
| Persistence | SQLite via xerial/sqlite-jdbc 3.45.2.0 |
| Build Tool | Apache Maven 3.9.9 (bundled in `.tools/`) |
| Test Framework | JUnit 5.10.2 |
| Packaging | maven-shade-plugin (fat JAR) |
| Distribution | Standalone runnable JAR (no install required) |

---

## 5. Project Structure

```
Package/
+-- pom.xml                              # Maven build descriptor
+-- mvn.cmd / mvnw.cmd                   # Local Maven wrappers
+-- run.bat / run.sh                     # One-click launch (Windows / macOS-Linux)
+-- README.md                            # This file
+-- SYLLABUS_MAPPING.md                  # Where each syllabus topic is in the code
+-- .tools/apache-maven-3.9.9/          # Bundled Maven binary
+-- target/
|   +-- resourceflow-1.0.0.jar          # Runnable fat JAR (built)
+-- src/
    +-- main/
    |   +-- java/com/resourceflow/
    |   |   +-- Launcher.java            # JAR entry point (GUI or --console)
    |   |   +-- MainApp.java             # JavaFX Application
    |   |   +-- model/                   # Domain entities and enums
    |   |   +-- strategy/                # Allocation algorithms + @StrategyInfo annotation
    |   |   +-- engine/                  # Orchestration engine, parallel runner, callback interface
    |   |   +-- constraint/              # Constraint validation
    |   |   +-- evaluation/              # Metrics and comparison
    |   |   +-- database/                # SQLite connection and schema
    |   |   +-- repository/              # Data access layer
    |   |   +-- service/                 # Business logic services
    |   |   +-- exception/               # Custom exception hierarchy (checked + unchecked)
    |   |   +-- io/                      # Scenario files (serialization), CSV reports (NIO)
    |   |   +-- console/                 # Text-mode front end
    |   |   +-- util/                    # DemoDataSeeder, AuditLog, generic EventBuffer<T>
    |   |   +-- ui/                      # JavaFX UI (10 views)
    |   +-- resources/css/styles.css    # Modern dark theme stylesheet
    +-- test/
        +-- java/com/resourceflow/
            +-- engine/            # AllocationEngineTest (11), ParallelComparisonTest (3)
            +-- strategy/          # StrategyHierarchyTest (6)
            +-- service/           # CapacityTransferTest (3, incl. deadlock test)
            +-- io/                # ScenarioFileStoreTest (3), ReportExporterTest (2)
            +-- util/              # AuditLogTest (4, producer-consumer)
            +-- evaluation/        # WeightedScoreTest (3)
            +-- repository/        # PersistenceTest (4)
```

---

## 6. Running the Application

### Option A - One-Click (Recommended)
Windows: double-click **`run.bat`**, or from Command Prompt in the project folder:
```bat
run.bat
run.bat --console
```
macOS / Linux:
```bash
./run.sh
./run.sh --console
```

### Option B - Direct JAR execution
```bash
java -jar target/resourceflow-1.0.0.jar             # desktop app
java -jar target/resourceflow-1.0.0.jar --console   # text menu in the terminal
```
The JAR contains JavaFX for Windows, Apple Silicon macOS and Linux. On an Intel Mac, rebuild it first (Option D).

### Option C - Maven JavaFX plugin
```bat
mvn.cmd javafx:run
```

### Option D - Rebuild the JAR
```bash
mvn package          # Windows: mvn.cmd package   (Maven is bundled in .tools/)
```

### Prerequisites
- **Java 21 or later** must be on your PATH
  - Check: `java -version`
- No other software installation required (Maven is bundled)

### First Launch
On first launch, ResourceFlow automatically:
1. Creates a local SQLite database `resourceflow.db` and an `audit.log` in the user data folder
   (Windows `%APPDATA%\ResourceFlow`, macOS `~/Library/Application Support/ResourceFlow`, Linux `~/.local/share/ResourceFlow`)
2. Seeds 5 sample resources and 11 sample requests so the UI is immediately populated with realistic data

---

## 7. Running the Tests

```bat
mvn.cmd test
```

**Test Results (last run):**
| Test Suite | Tests | What it checks |
|---|---|---|
| AllocationEngineTest | 11 | Strategies, constraints, metrics, explanations, adaptive diff |
| StrategyHierarchyTest | 6 | Annotation metadata, runtime polymorphism, inheritance chain |
| ParallelComparisonTest | 3 | Threads give same results as sequential; callback per thread; chained failure |
| CapacityTransferTest | 3 | Transfer rules; 20,000 opposing transfers on two threads without deadlock |
| ScenarioFileStoreTest | 3 | Serialization round trip; corrupt and missing files |
| ReportExporterTest | 2 | CSV layout and escaping |
| AuditLogTest | 4 | Producer–consumer buffer; logging from many threads |
| WeightedScoreTest | 3 | Weighted score, recommendation, re-planning capacity |
| PersistenceTest | 4 | SQLite CRUD and run history |
| **Total** | **39** | **0 failures, 0 errors** |

**Test coverage includes:**
- Priority-Based strategy with full/partial/no-capacity scenarios
- Fair-Share strategy with equal and unequal demand distributions
- Proportional strategy with single-resource and multi-resource scenarios
- Constraint validator hard-limit enforcement
- Evaluator metric computation (utilisation, fulfilment, fairness)
- Adaptive reallocator diff detection
- SQLite persistence: resources, requests, allocation runs (CRUD + round-trip)

---

## 8. Allocation Strategies

### Priority-Based (PRIORITY_BASED)
Ranks requests by priority (5 = critical, 1 = low) using a `PriorityQueue`, then by arrival time. Each request receives up to its demanded quantity, subject to available capacity. Higher-priority requests are fully satisfied before lower-priority ones receive anything.

**Best for:** Emergency resource pools, critical infrastructure, triage systems.

### Fair-Share (FAIR_SHARE)
Max-min "water-filling": every request is offered an equal share; requests that need less take only what they need and the surplus is re-shared among the rest. If a share breaks an all-or-nothing or minimum-quantity rule, the worst violator is removed and the capacity re-shared.

**Best for:** Multi-tenant shared infrastructure, cloud resource pools, equitable distribution.

### Proportional (PROPORTIONAL)
Each request gets capacity × (demand × priority) / Σ(demand × priority), capped at its own demand. Constraint violators are removed and the capacity re-split, as in Fair-Share.

**Best for:** Load-balanced systems, weighted bandwidth allocation, budget distribution.

---

## 9. Database Schema

SQLite file: `resourceflow.db` (in the user data folder, see First Launch)

| Table | Purpose |
|---|---|
| `resources` | Resource inventory (id, name, category, total/available capacity, unit) |
| `allocation_requests` | Demand requests (id, resource_id, requester, quantity, priority, status) |
| `allocation_runs` | Historical allocation run records (id, strategy, timestamp, notes) |
| `allocations` | Per-request allocation outcomes linked to a run |
| `allocation_metrics` | Utilisation, fulfilment, fairness and counts per run |
| `scenarios` | What-If scenario names and descriptions (full scenario contents are saved as `.rflow` files) |

All tables include `created_at` / `updated_at` timestamps. Schema is automatically initialised (idempotently) on every application start via `DatabaseManager.initializeSchema()`.

---

## 10. UI Screens

| Screen | Description |
|---|---|
| **Dashboard** | Live KPI cards (resources, requests, allocation runs), bar chart of resource utilisation, recent activity list |
| **Resources** | Full CRUD table for resources: add, edit (inline form), delete, search/filter |
| **Requests** | Full CRUD table for allocation requests with status badges and priority indicators |
| **Allocation Workspace** | Select strategy and constraint config, run allocation, view plan results in a table with per-item status |
| **Strategy Comparison** | Runs the 3 strategies on 3 threads (live execution log), weight sliders, weighted recommendation, complexity column |
| **Adaptive Reallocation** | Trigger reallocation based on current inventory state, view resource change diff |
| **What-If Analysis** | Editable capacities/demands/priorities, capacity transfer, sandbox simulation, scenario file export/import |
| **Decision Explanations** | Inspect per-request reasoning for the latest allocation plan |
| **History & Audit** | Browse all past allocation runs with expandable detail views |
| **Settings & Diagnostics** | DB file path, re-seed demo data, live audit log viewer, application info |

---

## 11. Syllabus Mapping

See **[SYLLABUS_MAPPING.md](SYLLABUS_MAPPING.md)** for every 23XT58 syllabus topic with the file and line
that demonstrates it, the two tutorial practice exercises, a 5-minute demo script and likely viva questions.

---

## Build Information

- **Packaged with:** maven-shade-plugin 3.5.2 (fat JAR, all dependencies bundled)
- **JAR size:** ~24 MB (JavaFX for Windows, Apple Silicon macOS and Linux, plus the SQLite JDBC driver)
- **Main-Class (JAR manifest):** `com.resourceflow.Launcher`
- **Main-Class (JavaFX):** `com.resourceflow.MainApp`
