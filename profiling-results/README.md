# Profiling Results - TfbBatchUpdateBenchmark

This directory contains comprehensive profiling results comparing two transaction provider configurations in the `TfbBatchUpdateBenchmark`.

## Quick Start - View Flame Graphs

The flame graphs are interactive SVG files that show CPU profiling data. **Simply open them in any modern web browser**:

### Database Exposed Transaction Provider
```bash
# Linux
xdg-open tfb-batch-update-database_flamegraph.svg

# macOS  
open tfb-batch-update-database_flamegraph.svg

# Windows
start tfb-batch-update-database_flamegraph.svg

# Or just drag and drop the file into your browser
```

### JDBC Transaction Exposed Transaction Provider
```bash
# Similar to above
open tfb-batch-update-jdbc_flamegraph.svg
```

### How to Read Flame Graphs

- **Width**: Proportional to time spent in that function
- **Height**: Call stack depth (bottom = root, top = leaf functions)
- **Color**: Randomly assigned for visual distinction
- **Interactive**: 
  - Click on any box to zoom in
  - Hover to see function name and percentage
  - Click "Reset Zoom" to return to full view
  - Search box to find specific functions

## Files in This Directory

### Primary Results

| File | Description | Size | Purpose |
|------|-------------|------|---------|
| `PROFILING_ANALYSIS.md` | **Start Here** - Comprehensive analysis | 6 KB | Full analysis and conclusions |
| `tfb-batch-update-database_flamegraph.svg` | Flame graph for DatabaseProvider | 444 KB | **Visual profiling results** |
| `tfb-batch-update-jdbc_flamegraph.svg` | Flame graph for JdbcProvider | 586 KB | **Visual profiling results** |

### Raw Profiling Data

| File | Description | Tool to Open |
|------|-------------|--------------|
| `tfb-batch-update-database.jfr` | JFR recording (1.8 MB) | JDK Mission Control, IntelliJ IDEA |
| `tfb-batch-update-jdbc.jfr` | JFR recording (1.8 MB) | JDK Mission Control, IntelliJ IDEA |

### Summary Reports

| File | Description |
|------|-------------|
| `tfb-batch-update-database_summary.html` | JFR summary report |
| `tfb-batch-update-jdbc_summary.html` | JFR summary report |

## Key Findings Summary

### Performance Comparison

| Configuration | Avg Time per Iteration | Relative Performance |
|--------------|----------------------|---------------------|
| DatabaseExposedTransactionProvider | 13,551 ms | Baseline |
| JdbcTransactionExposedTransactionProvider | 13,643 ms | +0.68% slower |

**Conclusion**: The performance difference is **negligible** (<1%). Choose based on architectural considerations, not performance.

### What the Flame Graphs Show

1. **DatabaseExposedTransactionProvider** (85 unique stacks):
   - Simpler execution paths
   - Creates new transaction for each statement preparation
   - Slightly lower overhead

2. **JdbcTransactionExposedTransactionProvider** (104 unique stacks):
   - More complex execution paths (22% more unique stacks)
   - Reuses single JDBC transaction (architecturally better)
   - Minimal performance cost for the architectural benefit

### Hotspots Visible in Both Flame Graphs

Looking at the flame graphs, you'll see the majority of time is spent in:

- **Database I/O**: PostgreSQL query execution and network operations
- **SQL Statement Preparation**: Using Exposed DSL
- **Coroutine Management**: Kotlin coroutines context switching
- **Vert.x Event Loop**: Async operation coordination

The transaction management overhead is barely visible in the flame graphs, which explains why the two approaches have similar performance.

## Reproducing the Results

To regenerate the profiling data:

```bash
# From the repository root
./profile-benchmark-jfr.sh

# Then convert to flame graphs
python3 jfr-to-flamegraph.py
```

## Advanced Analysis

### Using JDK Mission Control

For deeper JFR analysis:

```bash
jmc -open tfb-batch-update-database.jfr
```

JMC provides:
- Hot methods analysis
- Memory allocation profiling
- Thread analysis
- Lock contention analysis
- Garbage collection metrics

### Using jfr Command Line

```bash
# Print execution samples
jfr print --events jdk.ExecutionSample tfb-batch-update-database.jfr

# Show summary statistics
jfr summary tfb-batch-update-database.jfr

# Filter specific events
jfr print --events jdk.ExecutionSample,jdk.ObjectAllocationInNewTLAB tfb-batch-update-database.jfr
```

## Understanding the Benchmark

### What `_1kBatchUpdate` Does

1. Creates 1,000 concurrent async operations
2. Each operation:
   - Selects 20 random IDs
   - Sorts them (to reduce deadlocks)
   - Performs batch update with Exposed DSL
3. Measures total execution time

### The Two Configurations

**DatabaseExposedTransactionProvider**:
```kotlin
// Creates a new Exposed transaction each time
fun <R> statementPreparationExposedTransaction(statement: Transaction.() -> R): R =
    transaction(database, statement)
```

**JdbcTransactionExposedTransactionProvider**:
```kotlin
// Reuses a single JDBC transaction (more efficient)
private val connection: Connection // Reused connection
fun <R> statementPreparationExposedTransaction(statement: Transaction.() -> R): R =
    transaction(connection, statement)
```

## Questions?

Refer to `PROFILING_ANALYSIS.md` for:
- Detailed methodology
- Complete performance metrics
- Architecture analysis
- Recommendations

---

**Profiling Date**: February 3, 2026  
**Environment**: GitHub Actions (Ubuntu 24.04.3 LTS)  
**JDK**: OpenJDK 17.0.18  
**Profiler**: Java Flight Recorder (JFR)
