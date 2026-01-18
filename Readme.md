Factory Data Ingestion System - Technical Documentation

> Architecture:

The system follows a Standard 3-Tier Spring Boot Architecture designed for high-throughput RESTful ingestion.

- API Layer (Controller): Handles JSON mapping and asynchronous request entry.

 - Service Layer (Logic): Manages transactional boundaries, data validation, and the deduplication algorithm.

- Data Layer (Repository): Interfaces with an H2 In-Memory Database using JPA/Hibernate for low-latency persistence.

> Dedupe/Update Logic: 

The system implements a Payload-Aware Deduplication strategy to ensure data integrity:

- Identity Check: We first look up the record by its eventId.

- Comparison: If found, we compare the incoming fields (machineId, durationMs, defectCount, and eventTime) with the stored version.

> Decision Matrix:

- Identical: If all fields match, the event is ignored and marked as deduped.

- Different Payload: If any field differs, the record is updated to the latest state and marked as updated.

- "Winning" Record: The most recent API call with a modified payload "wins" by overwriting the previous entry, ensuring the database reflects the most current sensor reading.

> Thread-Safety

Thread safety is achieved through a multi-layered approach to handle up to 20 parallel requests:

- Database Constraints: The eventId is defined as the Primary Key. This acts as a final barrier; the database engine will physically block two threads from inserting the same ID at once.

- Transactional Semantics: The @Transactional annotation ensures that the "Check-then-Act" (read then save) logic is atomic.

- Row-Level Locking: During an update, Hibernate/H2 applies a row-level lock, preventing "Lost Updates" where two threads try to modify the same record simultaneously.

> Data Model

### Database Schema: `machine_events`

| Column | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `event_id` | **VARCHAR** | `PRIMARY KEY` | Unique identifier from the sensor. |
| `machine_id` | **VARCHAR** | `NOT NULL` | ID of the machine generating data. |
| `line_id` | **VARCHAR** | `INDEXED` | Derived during ingestion for analytics. |
| `event_time` | **TIMESTAMP** | `INDEXED` | When the event actually occurred. |
| `duration_ms` | **INTEGER** | - | Operation duration in milliseconds. |
| `defect_count` | **INTEGER** | - | Number of defects (-1 if unavailable). |

> Performance Strategy

To meet the requirement of 1,000 events in < 1 second, the following optimizations were used:

- Hibernate JDBC Batching: Configured hibernate.jdbc.batch_size: 1000. This reduces network overhead by sending one massive SQL command instead of 1,000 individual ones.

- In-Memory Storage: Using H2 in-memory mode removes Disk I/O bottlenecks.

- Query Optimization: Analytics use Native SQL aggregates (SUM, COUNT) to ensure the database does the "heavy lifting" instead of processing lists in Java memory.

> Edge Cases & Assumptions

- Defect Count -1: We assumed that -1 indicates a sensor malfunction. These events are stored and counted in eventsCount, but are ignored in defectsCount and rate calculations.

- Future Events: We reject events with a timestamp more than 15 minutes in the future to account for slight clock drift while preventing invalid data.

- Inclusive/Exclusive Boundaries: For the stats API, the start time is inclusive (>=) and the end time is exclusive (<) to prevent double-counting events that fall exactly on the hour.

> Setup & Run Instructions

- Prerequisites: Java 17 or 21 and Maven installed.

- Compile: mvn clean install

- Run Application: mvn spring-boot:run

- Run Tests (Benchmark): mvn test

- Access Database: Navigate to http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:factory_db).

> What I Would Improve with More Time

- Distributed Caching: Introduce Redis to store the eventIds. Checking a cache is faster than a database lookup for deduplication.

- Message Queue: For truly massive scale (100k+ events/sec), I would move the ingestion to Apache Kafka to decouple the API from the database.

- Circuit Breaker: Add Resilience4j to handle scenarios where the database might become slow, preventing the entire API from hanging.