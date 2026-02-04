# Profiling Results for TfbBatchUpdateBenchmark

This directory contains profiling results comparing the performance of two different `StatementPreparationExposedTransactionProvider` implementations:

1. **DatabaseExposedTransactionProvider** - Creates a new transaction for each SQL preparation call (traditional approach)
2. **JdbcTransactionExposedTransactionProvider** - Reuses a single JDBC transaction for all SQL preparation calls (performance optimization hypothesis being tested)

## Flame Graphs

The profiling was performed using [async-profiler](https://github.com/async-profiler/async-profiler) v3.0 with itimer-based CPU sampling. Each provider was profiled with 1000 concurrent batch updates (20 UPDATE statements each).

### DatabaseExposedTransactionProvider Flame Graph

![DatabaseExposedTransactionProvider Flame Graph](https://github.com/user-attachments/assets/f28a8575-80ab-496f-9bed-4203d94b844e)

### JdbcTransactionExposedTransactionProvider Flame Graph

![JdbcTransactionExposedTransactionProvider Flame Graph](https://github.com/user-attachments/assets/fc8454de-5701-444c-b6b0-d5c2207d317b)

### Files

- `database_provider_profile.html` - Interactive flame graph for `WithDatabaseExposedTransactionProvider`
- `jdbc_provider_profile.html` - Interactive flame graph for `WithJdbcTransactionExposedTransactionProvider`

### How to View

Open the HTML files in a web browser. The flame graphs are interactive:
- Click on any stack frame to zoom in
- Click "Reset Zoom" to zoom out
- Use the search box to highlight specific functions/methods
- Hover over frames to see timing information

### Key Differences to Look For

When comparing the two flame graphs, look for:

1. **Transaction creation overhead**: The DatabaseExposedTransactionProvider creates a new transaction for each `statementPreparationExposedTransaction` call, which shows up as more time spent in `org.jetbrains.exposed.v1.jdbc.transactions.transaction` calls.

2. **ThreadLocal operations**: The JdbcTransactionExposedTransactionProvider uses `withThreadLocalTransaction` which may show different patterns of ThreadLocal access.

3. **Connection pool activity**: DatabaseExposedTransactionProvider may show more activity related to acquiring/releasing connections from the pool.

## Profiling Configuration

- **Tool**: async-profiler 3.0
- **Event type**: itimer (interval timer - used when perf_events is not available)
- **Batch size**: 1000 concurrent async updates, 20 UPDATE statements each
- **Database**: PostgreSQL via Testcontainers

## Regenerating the Profiles

To regenerate the profiles, run:

```bash
# Download async-profiler
cd /tmp
curl -L https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz -o async-profiler.tar.gz
tar -xzf async-profiler.tar.gz

# Run profiling for DatabaseExposedTransactionProvider
./gradlew :exposed-vertx-sql-client-integrated:profileWithDatabaseProvider \
    -PasyncProfilerPath=/tmp/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so

# Run profiling for JdbcTransactionExposedTransactionProvider
./gradlew :exposed-vertx-sql-client-integrated:profileWithJdbcProvider \
    -PasyncProfilerPath=/tmp/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so
```
