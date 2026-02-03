#!/bin/bash

set -e

# Profiling script for TfbBatchUpdateBenchmark using Java Flight Recorder (JFR)
# JFR doesn't require special permissions and works in restricted environments

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
PROFILING_DIR="$PROJECT_ROOT/profiling-results"

mkdir -p "$PROFILING_DIR"

echo "========================================"
echo "TfbBatchUpdate Profiling Script (JFR)"
echo "========================================"

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
    
    # Run with JFR profiling
    java -cp "$CLASSPATH" \
        -XX:StartFlightRecording=filename=${OUTPUT_PREFIX}.jfr,settings=profile,dumponexit=true \
        com.huanshankeji.exposedvertxsqlclient.profiling.TfbBatchUpdateProfiler \
        "$PROVIDER_TYPE"
    
    echo ""
    echo "JFR recording saved to: ${OUTPUT_PREFIX}.jfr"
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
echo "JFR Recordings:"
echo "  - DatabaseExposedTransactionProvider: ${PROFILING_DIR}/tfb-batch-update-database.jfr"
echo "  - JdbcTransactionExposedTransactionProvider: ${PROFILING_DIR}/tfb-batch-update-jdbc.jfr"
echo ""
echo "Converting to flame graphs..."

# Download and use jfr-to-flame-graph converter if available
CONVERTER_URL="https://github.com/chrishantha/jfr-flame-graph/releases/download/v1.1.2/jfr-flame-graph-1.1.2.jar"
CONVERTER_JAR="${PROFILING_DIR}/jfr-flame-graph.jar"

if [ ! -f "$CONVERTER_JAR" ]; then
    echo "Downloading JFR to Flame Graph converter..."
    wget -q -O "$CONVERTER_JAR" "$CONVERTER_URL" || echo "WARNING: Could not download converter"
fi

if [ -f "$CONVERTER_JAR" ]; then
    echo "Generating flame graph for DatabaseExposedTransactionProvider..."
    java -jar "$CONVERTER_JAR" -f ${PROFILING_DIR}/tfb-batch-update-database.jfr -o ${PROFILING_DIR}/tfb-batch-update-database_flamegraph.html || echo "WARNING: Conversion failed"
    
    echo "Generating flame graph for JdbcTransactionExposedTransactionProvider..."
    java -jar "$CONVERTER_JAR" -f ${PROFILING_DIR}/tfb-batch-update-jdbc.jfr -o ${PROFILING_DIR}/tfb-batch-update-jdbc_flamegraph.html || echo "WARNING: Conversion failed"
    
    echo ""
    echo "Flame Graphs:"
    echo "  - DatabaseExposedTransactionProvider: ${PROFILING_DIR}/tfb-batch-update-database_flamegraph.html"
    echo "  - JdbcTransactionExposedTransactionProvider: ${PROFILING_DIR}/tfb-batch-update-jdbc_flamegraph.html"
fi

echo ""
echo "You can also analyze the .jfr files with JDK Mission Control or other JFR tools."
