#!/bin/bash

set -e

# Profiling script for TfbBatchUpdateBenchmark
# This script downloads async-profiler and profiles both configurations

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
PROFILING_DIR="$PROJECT_ROOT/profiling-results"
ASYNC_PROFILER_VERSION="3.0"
ASYNC_PROFILER_DIR="$PROFILING_DIR/async-profiler-${ASYNC_PROFILER_VERSION}-linux-x64"

mkdir -p "$PROFILING_DIR"

echo "========================================"
echo "TfbBatchUpdate Profiling Script"
echo "========================================"

# Download async-profiler if not already present
if [ ! -d "$ASYNC_PROFILER_DIR" ]; then
    echo "Downloading async-profiler ${ASYNC_PROFILER_VERSION}..."
    cd "$PROFILING_DIR"
    wget -q "https://github.com/async-profiler/async-profiler/releases/download/v${ASYNC_PROFILER_VERSION}/async-profiler-${ASYNC_PROFILER_VERSION}-linux-x64.tar.gz"
    tar -xzf "async-profiler-${ASYNC_PROFILER_VERSION}-linux-x64.tar.gz"
    rm "async-profiler-${ASYNC_PROFILER_VERSION}-linux-x64.tar.gz"
    echo "async-profiler downloaded successfully"
else
    echo "async-profiler already downloaded"
fi

PROFILER_LIB="$ASYNC_PROFILER_DIR/lib/libasyncProfiler.so"

if [ ! -f "$PROFILER_LIB" ]; then
    echo "ERROR: async-profiler library not found at $PROFILER_LIB"
    exit 1
fi

cd "$PROJECT_ROOT"

# Build the project
echo ""
echo "Building the project..."
./gradlew :exposed-vertx-sql-client-integrated:benchmarksClasses --no-daemon -q

CLASSPATH=$(./gradlew :exposed-vertx-sql-client-integrated:printBenchmarksClasspath --no-daemon -q)

function profile_configuration() {
    local PROVIDER_TYPE=$1
    local PROVIDER_NAME=$2
    local OUTPUT_PREFIX="${PROFILING_DIR}/tfb-batch-update-${PROVIDER_TYPE}"
    
    echo ""
    echo "========================================"
    echo "Profiling with ${PROVIDER_NAME}"
    echo "========================================"
    
    # Run with async-profiler using --all-user mode (doesn't require perf events)
    java -cp "$CLASSPATH" \
        -agentpath:"${PROFILER_LIB}=start,event=cpu,alluser,interval=1000000,file=${OUTPUT_PREFIX}.html,flamegraph" \
        com.huanshankeji.exposedvertxsqlclient.profiling.TfbBatchUpdateProfiler \
        "$PROVIDER_TYPE"
    
    echo ""
    echo "Flame graph saved to: ${OUTPUT_PREFIX}.html"
}

# Profile both configurations
profile_configuration "database" "WithDatabaseExposedTransactionProvider"
profile_configuration "jdbc" "WithJdbcTransactionExposedTransactionProvider"

echo ""
echo "========================================"
echo "Profiling Complete!"
echo "========================================"
echo "Results saved in: $PROFILING_DIR"
echo ""
echo "Flame Graphs:"
echo "  - DatabaseExposedTransactionProvider: ${PROFILING_DIR}/tfb-batch-update-database.html"
echo "  - JdbcTransactionExposedTransactionProvider: ${PROFILING_DIR}/tfb-batch-update-jdbc.html"
echo ""
echo "You can open these HTML files in a browser to view the flame graphs."
