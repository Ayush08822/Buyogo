# Performance Benchmark Report

### System Specifications
- **CPU:** 11th Gen Intel(R) Core(TM) i5-1135G7 @ 2.40GHz   2.42 GHz
- **RAM:** 16 GB
- **OS:** [Windows/macOS/Linux]

### Benchmark Execution
- **Command:** `mvn test -Dtest=BuyogoApplicationTests`
- **Scope:** Ingestion of 1,000 unique MachineEvent objects via POST `/events/batch`.

### Results
- **Batch Size:** 1,000 events
- **Total Time:**  572ms
- **Requirement:** < 1000 ms (Status: **PASSED**)

### Optimization Strategy
1. **Batching:** Configured Hibernate to use a batch size of 1000 to reduce network round-trips.
2. **Persistence:** Leveraged H2 In-Memory DB for high-speed volatile storage.
3. **Indexing:** Leveraged Primary Key constraints for O(1) deduplication check.