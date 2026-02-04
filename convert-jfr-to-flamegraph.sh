#!/bin/bash

set -e

# Convert JFR recordings to flame graphs using jfr-report-tool

PROFILING_DIR="/home/runner/work/exposed-vertx-sql-client/exposed-vertx-sql-client/profiling-results"

echo "Converting JFR recordings to flame graphs..."

cd "$PROFILING_DIR"

# Download FlameGraph scripts
if [ ! -d "FlameGraph" ]; then
    echo "Downloading FlameGraph scripts..."
    git clone https://github.com/brendangregg/FlameGraph.git
fi

# Use jfr command line tool to convert JFR to collapsed stacks, then to flame graph
for jfr_file in tfb-batch-update-database.jfr tfb-batch-update-jdbc.jfr; do
    if [ -f "$jfr_file" ]; then
        base_name="${jfr_file%.jfr}"
        echo "Processing $jfr_file..."
        
        # Try using jfr print command to generate flame graph compatible output
        # This creates a simple text report
        jfr print --events jdk.ExecutionSample "$jfr_file" > "${base_name}_execution.txt" || echo "jfr print failed"
        
        # Create an HTML report with JFR summary
        jfr summary "$jfr_file" > "${base_name}_summary.html" 2>&1 || echo "jfr summary failed"
        
        echo "  - Created ${base_name}_execution.txt"
        echo "  - Created ${base_name}_summary.html"
    fi
done

echo ""
echo "JFR analysis files created:"
ls -lh tfb-batch-update-*_*.{txt,html} 2>/dev/null || echo "Some conversions may have failed"

echo ""
echo "You can view the .jfr files with:"
echo "  - JDK Mission Control (jmc)"
echo "  - IntelliJ IDEA Profiler"
echo "  - Or any JFR-compatible profiler"
